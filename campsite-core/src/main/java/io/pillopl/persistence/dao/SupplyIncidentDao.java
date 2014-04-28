package io.pillopl.persistence.dao;

import io.pillopl.supply.SupplyIncident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SupplyIncidentDao extends JpaRepository<SupplyIncident, Long> {

    List<SupplyIncident> findByPedestalCodeOrderByCreateDateDesc(String pedestalCode);

    List<SupplyIncident> findByCreateDateBetweenOrderByCreateDate(LocalDateTime from, LocalDateTime to);
}
