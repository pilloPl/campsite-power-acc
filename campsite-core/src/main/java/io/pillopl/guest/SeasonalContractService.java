package io.pillopl.guest;

import io.pillopl.card.CampingCard;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.order.OrderItem;
import io.pillopl.order.ProductDefinition.FixedPricePeriod;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.SeasonalContractDao;
import io.pillopl.pricing.SeasonalContractTariffProvider;
import io.pillopl.pricing.Tariff;
import io.pillopl.util.DateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Seasonal contracts: created from the seasonal package of an activated order, terminated at the resident's request,
 * and the first step of the tariff resolution: the tariff of the contract running for the card's guest overrides
 * every tariff of the pedestal (decision 2 of the report).
 */
@Primary
@Service
public class SeasonalContractService implements SeasonalContractTariffProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SeasonalContractService.class);

    private final SeasonalContractDao seasonalContractDao;

    private final CampingCardDao campingCardDao;

    private final GroupService groupService;

    private final DateService dateService;

    public SeasonalContractService(SeasonalContractDao seasonalContractDao, CampingCardDao campingCardDao,
                                   GroupService groupService, DateService dateService) {
        this.seasonalContractDao = seasonalContractDao;
        this.campingCardDao = campingCardDao;
        this.groupService = groupService;
        this.dateService = dateService;
    }

    /**
     * The contract tariff applies when the card's guest has a contract running on the day, the card is billed to the
     * contract's account, and the socket lies on the contract's pitch (or anywhere on its campsite for a contract
     * without pitch, anywhere for a contract without campsite). The latest started contract wins.
     */
    @Override
    @Transactional(readOnly = true)
    public Tariff findContractTariff(Socket socket, Long cardId, LocalDateTime at) {
        if (cardId == null || at == null) {
            return null;
        }
        CampingCard card = campingCardDao.findById(cardId).orElse(null);
        if (card == null || card.getGuest() == null) {
            return null;
        }
        LocalDate day = at.toLocalDate();
        for (SeasonalContract contract : seasonalContractDao.findStartedContractsOfGuest(card.getGuest().getId(),
                day)) {
            if (!contract.isActiveOn(day)) {
                continue;
            }
            if (!contract.getBillingAccount().hasErpAccountNo(card.getBillingAccountNo())) {
                LOG.debug(String.format("Card %s is not billed to the account of contract %s", card, contract));
                continue;
            }
            if (!coversSocket(contract, socket)) {
                LOG.debug(String.format("Socket %s is not on the pitch of contract %s", socket, contract));
                continue;
            }
            LOG.debug(String.format("Contract %s gives tariff '%s' to card %s", contract,
                    contract.getTariff().getName(), card));
            return contract.getTariff();
        }
        return null;
    }

    boolean coversSocket(SeasonalContract contract, Socket socket) {
        Group siteGroup = contract.getSiteGroup();
        if (siteGroup == null) {
            return true;
        }
        Pedestal pedestal = socket != null ? socket.getPedestal() : null;
        if (pedestal == null || pedestal.getGroup() == null
                || !groupService.findManageable(siteGroup).contains(pedestal.getGroup().getId())) {
            return false;
        }
        return contract.getPitchNo() == null || contract.getPitchNo().equalsIgnoreCase(socket.getPitchNo());
    }

    /**
     * The contract of an activated order's seasonal package: the pitch and season of the item, the fee and kWh
     * package as ordered, the tariff of the product.
     */
    @Transactional
    public SeasonalContract createContract(BillingAccount account, OrderItem item) {
        if (!item.isSeasonalPackage()) {
            throw new IllegalArgumentException("Order item " + item + " is no seasonal package");
        }
        Tariff tariff = item.getProductDefinition().getTariff();
        if (tariff == null) {
            throw new IllegalStateException("Seasonal package " + item.getProductDefinition().getCode()
                    + " has no tariff");
        }
        if (item.getPitchNo() != null) {
            for (SeasonalContract running : seasonalContractDao.findRunningContractsOnPitch(
                    item.getSiteGroup().getId(), item.getPitchNo())) {
                if (running.isActiveIn(item.getStartDate(), item.getEndDate() != null ? item.getEndDate()
                        : LocalDate.MAX)) {
                    throw new IllegalStateException(String.format("Pitch %s is let to contract %s",
                            item.getPitchNo(), running.getId()));
                }
            }
        }
        SeasonalContract contract = new SeasonalContract(account, tariff, item.getSiteGroup(), item.getPitchNo(),
                item.getStartDate(), item.getEndDate());
        contract.setOrderItem(item);
        contract.setFixedFee(item.getFixedAmount());
        contract.setFeePeriod(item.getFixedPricePeriod() == FixedPricePeriod.MONTHLY ? SeasonalContract.FeePeriod.MONTHLY
                : SeasonalContract.FeePeriod.SEASONAL);
        contract.addPackageItem(item.getProductDefinition(), item, item.getPackageKwh());
        contract.setCreateDate(dateService.getNow());
        contract = seasonalContractDao.save(contract);
        LOG.info(String.format("Contract %s created for account %s", contract, account));
        return contract;
    }

    /**
     * The resident leaves before the season ends; the contract (and its tariff) runs until the requested day.
     */
    @Transactional
    public SeasonalContract requestTermination(Long contractId, LocalDate requestedEnd) {
        SeasonalContract contract = contractId != null ? seasonalContractDao.findById(contractId).orElse(null) : null;
        if (contract == null) {
            throw new NoSuchElementException("Seasonal contract " + contractId + " not found");
        }
        contract.requestTermination(requestedEnd, dateService.getNow());
        return seasonalContractDao.save(contract);
    }

    /**
     * Contracts of a billing account running on some day of the period (the seasonal billing to the ERP).
     */
    @Transactional(readOnly = true)
    public List<SeasonalContract> findContractsActiveIn(Long billingAccountId, LocalDate from, LocalDate to) {
        return seasonalContractDao.findContractsOfAccountIn(billingAccountId, from, to).stream()
                .filter(contract -> contract.isActiveIn(from, to))
                .toList();
    }
}
