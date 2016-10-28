package io.pillopl.issuer;

import io.pillopl.card.CampingCard;
import io.pillopl.card.RfidConverter;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.consumption.ConsumptionRecord;
import io.pillopl.consumption.ConsumptionRecordApiService;
import io.pillopl.consumption.ExportStatus;
import io.pillopl.dto.consumption.ConsumptionRecordDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.attribute.WhitelistFilterService;
import io.pillopl.issuer.IssuerExchange.CardLine;
import io.pillopl.issuer.IssuerExchange.ExchangeResult;
import io.pillopl.issuer.IssuerSyncStatus.Direction;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import io.pillopl.persistence.dao.IssuerPartnerDao;
import io.pillopl.util.DateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The exchange with the issuer partners, run by the exchange job and from the console: the nightly sync state of the
 * cards, then per partner and enabled direction the card import, the record export and the record import, and the
 * card list export to all partners at once. The transport is the {@link IssuerExchange} of the partner's interface
 * version. Every direction records its run in {@link IssuerSyncStatus}; a failing partner does not stop the others.
 * <p>
 * The card list export works on the one sync state of a card: the changes go to every partner that takes the card
 * list, and the cards count as exported only when every partner got them. A partner that was not reachable makes
 * the next run send the same changes to all partners again.
 */
@Service
public class IssuerExchangeService {

    private static final Logger LOG = LoggerFactory.getLogger(IssuerExchangeService.class);

    static final int MAX_REJECTIONS_IN_MESSAGE = 5;

    static final DateTimeFormatter ACTION_DATE_FORMAT = DateTimeFormatter.ofPattern(IssuerExchange.ACTION_DATE_PATTERN);

    private final IssuerPartnerDao issuerPartnerDao;

    private final CampingCardDao campingCardDao;

    private final ConsumptionRecordDao consumptionRecordDao;

    private final IssuerCardImportService issuerCardImportService;

    private final IssuerExchangeImportService issuerExchangeImportService;

    private final WhitelistFilterService whitelistFilterService;

    private final GroupService groupService;

    private final SystemPropertyService systemPropertyService;

    private final DateService dateService;

    private final Map<IssuerPartner.InterfaceVersion, IssuerExchange> exchanges =
            new EnumMap<>(IssuerPartner.InterfaceVersion.class);

    private final TransactionTemplate transactionTemplate;

    public IssuerExchangeService(IssuerPartnerDao issuerPartnerDao, CampingCardDao campingCardDao,
                                 ConsumptionRecordDao consumptionRecordDao,
                                 IssuerCardImportService issuerCardImportService,
                                 IssuerExchangeImportService issuerExchangeImportService,
                                 WhitelistFilterService whitelistFilterService, GroupService groupService,
                                 SystemPropertyService systemPropertyService, DateService dateService,
                                 List<IssuerExchange> issuerExchanges,
                                 PlatformTransactionManager transactionManager) {
        this.issuerPartnerDao = issuerPartnerDao;
        this.campingCardDao = campingCardDao;
        this.consumptionRecordDao = consumptionRecordDao;
        this.issuerCardImportService = issuerCardImportService;
        this.issuerExchangeImportService = issuerExchangeImportService;
        this.whitelistFilterService = whitelistFilterService;
        this.groupService = groupService;
        this.systemPropertyService = systemPropertyService;
        this.dateService = dateService;
        for (IssuerExchange exchange : issuerExchanges) {
            IssuerExchange other = this.exchanges.put(exchange.getInterfaceVersion(), exchange);
            if (other != null) {
                throw new IllegalStateException(String.format("Two exchanges for interface version %s: %s and %s",
                        exchange.getInterfaceVersion(), other.getClass().getSimpleName(),
                        exchange.getClass().getSimpleName()));
            }
        }
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * One run of the whole exchange.
     *
     * @return the partners exchanged with
     */
    public List<IssuerPartner> exchangeAll() {
        int cards = transactionTemplate.execute(status ->
                campingCardDao.updateExpiredCardsPartnerSyncState(dateService.getToday()));
        LOG.info(String.format("Partner sync state of %s cards calculated", cards));

        List<IssuerPartner> partners = issuerPartnerDao.findExchanging();
        if (partners.isEmpty()) {
            LOG.info("No active issuer partner, nothing to exchange");
        } else {
            LOG.info(String.format("Exchange with %s issuer partners: %s", partners.size(),
                    partners.stream().map(IssuerPartner::getName).toList()));
        }
        for (IssuerPartner partner : partners) {
            if (partner.isCardImport()) {
                runDirection(partner.getId(), Direction.CARD_IMPORT, this::importCards);
            }
            if (partner.isRecordExport()) {
                runDirection(partner.getId(), Direction.RECORD_EXPORT, this::exportConsumptionRecords);
            }
            if (partner.isRecordImport()) {
                runDirection(partner.getId(), Direction.RECORD_IMPORT, this::importConsumptionRecords);
            }
        }
        exportCards(partners.stream().filter(IssuerPartner::isCardExport).toList());
        return issuerPartnerDao.findExchanging();
    }

    /**
     * Card list changes of the partner, each line imported on its own: a refused line does not stop the others.
     */
    ExchangeResult importCards(IssuerPartner partner) {
        List<CardLine> cardLines = getExchange(partner).fetchCards(partner);
        int accepted = 0;
        List<String> rejections = new ArrayList<>();
        for (CardLine cardLine : cardLines) {
            try {
                issuerCardImportService.importCard(partner, cardLine);
                accepted++;
            } catch (RuntimeException e) {
                LOG.warn(String.format("Card line of %s refused: %s", partner.getName(), e.getMessage()));
                rejections.add(e.getMessage());
            }
        }
        return new ExchangeResult(accepted, rejections.size(), toMessage(rejections));
    }

    /**
     * The records of the partner's members (guests in the partner's ISSUER subtree) waiting for export, sent and
     * marked TRANSFERRED in one transaction: a failed send leaves them waiting.
     */
    ExchangeResult exportConsumptionRecords(IssuerPartner partner) {
        return transactionTemplate.execute(status -> {
            LocalDateTime now = dateService.getNow();
            LocalDateTime from = now.minusDays(systemPropertyService.getIssuerRecordExportLookBackInDays());
            List<Long> ids = consumptionRecordDao.findConsumptionRecordIdsForExport(from, now, null,
                    partner.getIssuerGroup().getId());
            List<ConsumptionRecord> records = consumptionRecordDao.findAllById(ids);
            if (records.isEmpty()) {
                return ExchangeResult.sent(0);
            }
            List<ConsumptionRecordDTO> lines = new ArrayList<>(records.size());
            for (ConsumptionRecord record : records) {
                lines.add(ConsumptionRecordApiService.toDTO(record));
            }
            getExchange(partner).sendConsumptionRecords(partner, lines);
            for (ConsumptionRecord record : records) {
                record.setExportStatus(ExportStatus.TRANSFERRED);
            }
            consumptionRecordDao.saveAll(records);
            return ExchangeResult.sent(records.size());
        });
    }

    ExchangeResult importConsumptionRecords(IssuerPartner partner) {
        List<ConsumptionRecordDTO> lines = getExchange(partner).fetchConsumptionRecords(partner);
        if (lines.isEmpty()) {
            return ExchangeResult.sent(0);
        }
        List<ConsumptionRecordDTO> accepted = issuerExchangeImportService.importConsumptionRecords(partner.getId(),
                lines, partner.getName());
        int rejected = lines.size() - accepted.size();
        return new ExchangeResult(accepted.size(), rejected,
                rejected > 0 ? rejected + " records refused, see the log" : null);
    }

    /**
     * The network's card list changes to every partner taking the card list. Each partner gets the cards its
     * whitelist filters let through; the cards count as exported when every partner got its list.
     */
    void exportCards(List<IssuerPartner> partners) {
        if (partners.isEmpty()) {
            return;
        }
        Group networkCardGroup = groupService.fetchNetworkCardIssuerGroup();
        if (networkCardGroup == null) {
            LOG.error("No network card group, card list not exported");
            return;
        }
        transactionTemplate.executeWithoutResult(transaction -> {
            LocalDateTime now = dateService.getNow();
            List<CampingCard> cards = campingCardDao.findCardsForPartnerExport(networkCardGroup.getId());
            boolean allSent = true;
            for (IssuerPartner partner : partners) {
                List<CardLine> cardLines = new ArrayList<>();
                for (CampingCard card : cards) {
                    if (whitelistFilterService.isSynchronizableForExport(card.getGuest().getGroup().getId(), partner)) {
                        cardLines.add(toCardLine(card, now));
                    }
                }
                ExchangeResult result = null;
                String failure = null;
                try {
                    if (!cardLines.isEmpty()) {
                        getExchange(partner).sendCards(partner, cardLines);
                    }
                    result = ExchangeResult.sent(cardLines.size());
                } catch (RuntimeException e) {
                    LOG.error(String.format("Card list for %s not exported: %s", partner.getName(), e.getMessage()));
                    failure = e.getMessage();
                    allSent = false;
                }
                saveStatus(partner.getId(), Direction.CARD_EXPORT, result, failure, now);
            }
            if (allSent) {
                for (CampingCard card : cards) {
                    card.partnerSyncExported(now);
                }
                campingCardDao.saveAll(cards);
            } else {
                LOG.warn(String.format("%s cards stay waiting: not every partner got the card list", cards.size()));
            }
        });
    }

    static CardLine toCardLine(CampingCard card, LocalDateTime now) {
        // the billing account of a network card stays in the network
        return new CardLine(RfidConverter.decimalToHexBigEndian(card.getCardRfid()), card.getCardNumber(),
                card.getPartnerSyncState().name(), ACTION_DATE_FORMAT.format(now), null, card.getValidTo());
    }

    private void runDirection(Long partnerId, Direction direction, Function<IssuerPartner, ExchangeResult> step) {
        IssuerPartner partner = findPartner(partnerId);
        ExchangeResult result = null;
        String failure = null;
        try {
            result = step.apply(partner);
            LOG.info(String.format("%s with %s: %s", direction, partner.getName(), result));
        } catch (RuntimeException e) {
            LOG.error(String.format("%s with %s failed: %s", direction, partner.getName(), e.getMessage()), e);
            failure = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }
        LocalDateTime now = dateService.getNow();
        ExchangeResult finalResult = result;
        String finalFailure = failure;
        transactionTemplate.executeWithoutResult(status ->
                saveStatus(partnerId, direction, finalResult, finalFailure, now));
    }

    private IssuerPartner findPartner(Long partnerId) {
        return issuerPartnerDao.findById(partnerId).orElseThrow(() ->
                new IssuerExchangeException("Issuer partner " + partnerId + " no longer exists"));
    }

    private void saveStatus(Long partnerId, Direction direction, ExchangeResult result, String failure,
                            LocalDateTime now) {
        IssuerPartner partner = findPartner(partnerId);
        IssuerSyncStatus status = partner.getSyncStatus(direction);
        if (failure != null) {
            status.failed(now, failure);
        } else {
            status.succeeded(now, result.accepted(), result.rejected(), result.message());
        }
        issuerPartnerDao.save(partner);
    }

    IssuerExchange getExchange(IssuerPartner partner) {
        IssuerExchange exchange = exchanges.get(partner.getInterfaceVersion());
        if (exchange == null) {
            throw new IssuerExchangeException("No exchange for interface version " + partner.getInterfaceVersion()
                    + " of " + partner.getName());
        }
        return exchange;
    }

    private static String toMessage(List<String> rejections) {
        if (rejections.isEmpty()) {
            return null;
        }
        List<String> shown = rejections.subList(0, Math.min(MAX_REJECTIONS_IN_MESSAGE, rejections.size()));
        String message = String.join("; ", shown);
        return rejections.size() > shown.size() ? message + "; and " + (rejections.size() - shown.size()) + " more"
                : message;
    }
}
