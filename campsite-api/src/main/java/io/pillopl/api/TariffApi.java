package io.pillopl.api;

import io.pillopl.dto.pricing.PriceSimulationDTO;
import io.pillopl.dto.pricing.SocketTariffDTO;
import io.pillopl.dto.pricing.TariffApplicabilityDTO;
import io.pillopl.dto.pricing.TariffDTO;
import io.pillopl.value.GroupId;
import io.pillopl.value.Kwh;
import io.pillopl.value.PedestalCode;
import io.pillopl.value.SocketId;
import io.pillopl.value.TariffId;
import io.pillopl.value.Username;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tariffs of the market of the calling user, their assignment to sockets, pedestals and campsites, the tariff that
 * applies to a socket and the price simulation of a supply.
 */
public interface TariffApi {

    List<TariffDTO> findTariffs(Username authUsername);

    TariffDTO getTariff(Username authUsername, TariffId tariffId);

    /**
     * @return the default tariff of the user's market, null when the market has none
     */
    TariffDTO getDefaultTariff(Username authUsername);

    boolean existsTariff(Username authUsername, String tariffName);

    TariffDTO saveTariff(Username authUsername, TariffDTO tariff);

    /**
     * Only a tariff that is not in use can be changed.
     */
    TariffDTO updateTariff(Username authUsername, TariffId tariffId, TariffDTO tariff);

    TariffDTO setMarketDefaultTariff(Username authUsername, TariffId tariffId);

    /**
     * @param tariffId null removes the assignments planned from the start date on
     */
    List<TariffApplicabilityDTO> applyTariffToPedestals(Username authUsername, List<PedestalCode> pedestalCodes,
                                                        TariffId tariffId, LocalDateTime startDate);

    /**
     * @param tariffId null removes the assignments planned from the start date on
     * @return the new assignment, null when assignments were removed
     */
    TariffApplicabilityDTO applyTariffToSocket(Username authUsername, SocketId socketId, TariffId tariffId,
                                               LocalDateTime startDate);

    List<TariffApplicabilityDTO> applyTariffToAffiliatePedestals(Username authUsername, Username ownerUsername,
                                                                 TariffId tariffId, LocalDateTime startDate);

    /**
     * @param tariffId null removes the default of the group
     * @return the default tariff of the group now in effect (own or inherited), null when none
     */
    TariffDTO setCampsiteDefaultTariff(Username authUsername, GroupId groupId, TariffId tariffId);

    /**
     * @param at null for now
     */
    SocketTariffDTO findSocketTariff(Username authUsername, SocketId socketId, LocalDateTime at);

    /**
     * Prices a supply on the socket as it would be charged, with the resolved tariff or a given one.
     *
     * @param tariffId null for the tariff the socket would get
     * @param nights   null to count them from plug-in and plug-out
     */
    PriceSimulationDTO simulatePrice(Username authUsername, SocketId socketId, TariffId tariffId,
                                     LocalDateTime plugIn, LocalDateTime plugOut, Kwh kwh, Integer nights);
}
