package io.pillopl.persistence.dao;

import io.pillopl.group.Market;
import io.pillopl.pricing.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TariffDao extends JpaRepository<Tariff, Long> {

    @Query("select t from Tariff t where t.market = :market order by t.name")
    List<Tariff> findAllForMarket(@Param("market") Market market);

    @Query("select t from Tariff t where t.market = :market and t.name = :tariffName")
    Tariff findForNameAndMarket(@Param("market") Market market, @Param("tariffName") String tariffName);

    /**
     * The database allows one default tariff per market.
     */
    @Query("select t from Tariff t where t.market = :market and t.defaultTariff = true")
    Tariff findDefaultTariffForMarket(@Param("market") Market market);
}
