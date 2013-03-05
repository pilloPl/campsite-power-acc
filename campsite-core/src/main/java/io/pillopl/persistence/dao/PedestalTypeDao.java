package io.pillopl.persistence.dao;

import io.pillopl.infrastructure.PedestalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PedestalTypeDao extends JpaRepository<PedestalType, Long> {

    @Query("select t from PedestalType t where upper(t.manufacturer) = upper(:manufacturer) and upper(t.name) = upper(:name)")
    PedestalType findByManufacturerAndName(@Param("manufacturer") String manufacturer, @Param("name") String name);

    @Query("select t from PedestalType t where t.deleted = false order by t.manufacturer, t.name")
    List<PedestalType> findAllActive();
}
