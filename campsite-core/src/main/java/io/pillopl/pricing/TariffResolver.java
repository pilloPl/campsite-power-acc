package io.pillopl.pricing;

import io.pillopl.group.GroupService;
import io.pillopl.group.Market;
import io.pillopl.group.attribute.DefaultTariffService;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.TariffApplicabilityDao;
import io.pillopl.persistence.dao.TariffDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * The one way to find the tariff of a supply. Used by the start answer to the pedestal, the QR payment, the
 * consumption record and the seasonal billing. Order:
 * <ol>
 *     <li>tariff of the seasonal contract of the card's guest (overrides the pedestal),</li>
 *     <li>tariff assigned to the socket, then to the pedestal (newest start date not after the plug-in),</li>
 *     <li>default tariff of the campsite: the nearest {@code DefaultTariff} group attribute,</li>
 *     <li>default tariff of the market of the pedestal's group.</li>
 * </ol>
 */
@Service
@Transactional(readOnly = true)
public class TariffResolver {

    private static final Logger LOG = LoggerFactory.getLogger(TariffResolver.class);

    private final SeasonalContractTariffProvider seasonalContractTariffProvider;

    private final TariffApplicabilityDao tariffApplicabilityDao;

    private final DefaultTariffService defaultTariffService;

    private final TariffDao tariffDao;

    private final GroupService groupService;

    private final SocketDao socketDao;

    public TariffResolver(SeasonalContractTariffProvider seasonalContractTariffProvider,
                          TariffApplicabilityDao tariffApplicabilityDao, DefaultTariffService defaultTariffService,
                          TariffDao tariffDao, GroupService groupService, SocketDao socketDao) {
        this.seasonalContractTariffProvider = seasonalContractTariffProvider;
        this.tariffApplicabilityDao = tariffApplicabilityDao;
        this.defaultTariffService = defaultTariffService;
        this.tariffDao = tariffDao;
        this.groupService = groupService;
        this.socketDao = socketDao;
    }

    /**
     * @throws IllegalArgumentException when the socket does not exist
     * @throws NoTariffFoundException   when no step finds a tariff
     */
    public ResolvedTariff resolve(Long socketId, Long cardId, LocalDateTime at) {
        Socket socket = socketId != null ? socketDao.findById(socketId).orElse(null) : null;
        if (socket == null) {
            throw new IllegalArgumentException("Socket not found: " + socketId);
        }
        return resolve(socket, cardId, at);
    }

    /**
     * @param cardId null for supplies without a card (QR payment, free socket)
     * @throws NoTariffFoundException when no step finds a tariff
     */
    public ResolvedTariff resolve(Socket socket, Long cardId, LocalDateTime at) {
        if (socket == null || at == null) {
            throw new IllegalArgumentException("Socket and time are needed to resolve a tariff");
        }

        Tariff contractTariff = seasonalContractTariffProvider.findContractTariff(socket, cardId, at);
        if (contractTariff != null) {
            return found(socket, contractTariff, TariffSource.SEASONAL_CONTRACT);
        }

        TariffApplicability socketTariff = tariffApplicabilityDao.findActiveTariffForSocket(socket.getId(), at);
        if (socketTariff != null) {
            return found(socket, socketTariff.getTariff(), TariffSource.SOCKET);
        }

        Pedestal pedestal = socket.getPedestal();
        TariffApplicability pedestalTariff = tariffApplicabilityDao.findActiveTariffForPedestal(pedestal.getId(), at);
        if (pedestalTariff != null) {
            return found(socket, pedestalTariff.getTariff(), TariffSource.PEDESTAL);
        }

        if (pedestal.getGroup() == null) {
            LOG.warn(String.format("No tariff for socket %s: pedestal %s hangs in no group", socket.getId(), pedestal));
            throw new NoTariffFoundException(String.format("No tariff for socket %s: pedestal %s hangs in no group",
                    socket.getId(), pedestal.getCode()));
        }

        Tariff campsiteTariff = defaultTariffService.findDefaultTariff(pedestal.getGroup().getId());
        if (campsiteTariff != null) {
            return found(socket, campsiteTariff, TariffSource.CAMPSITE_DEFAULT);
        }

        Market market = groupService.determineMarketByGroupId(pedestal.getGroup().getId());
        Tariff marketTariff = market != null && market.isPrefixMarket() ? tariffDao.findDefaultTariffForMarket(market)
                : null;
        if (marketTariff != null) {
            return found(socket, marketTariff, TariffSource.MARKET_DEFAULT);
        }

        LOG.warn(String.format("No tariff found for socket %s (market %s)", socket.getId(), market));
        throw new NoTariffFoundException(String.format("No tariff found for socket %s of pedestal %s (market %s)",
                socket.getId(), pedestal.getCode(), market));
    }

    private static ResolvedTariff found(Socket socket, Tariff tariff, TariffSource source) {
        LOG.debug(String.format("Tariff '%s' for socket %s from %s", tariff.getName(), socket.getId(), source));
        return new ResolvedTariff(tariff, source);
    }
}
