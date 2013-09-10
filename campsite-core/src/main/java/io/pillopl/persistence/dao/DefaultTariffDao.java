package io.pillopl.persistence.dao;

import io.pillopl.group.attribute.DefaultTariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DefaultTariffDao extends JpaRepository<DefaultTariff, Long> {

    /**
     * Attributes of the given groups, not in the order of the ids: resolve with GroupAttributeHelper.findNearest.
     */
    @Query("select a from DefaultTariff a where a.group.id in (:groupIds)")
    List<DefaultTariff> findByGroupIds(@Param("groupIds") Collection<Long> groupIds);

    @Query("select a from DefaultTariff a where a.group.id = :groupId")
    DefaultTariff findByGroupId(@Param("groupId") Long groupId);

    @Query("select count(a) from DefaultTariff a where a.tariff.id = :tariffId")
    long countByTariffId(@Param("tariffId") Long tariffId);
}
