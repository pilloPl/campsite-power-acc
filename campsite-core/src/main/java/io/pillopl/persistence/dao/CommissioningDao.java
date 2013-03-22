package io.pillopl.persistence.dao;

import io.pillopl.infrastructure.Commissioning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommissioningDao extends JpaRepository<Commissioning, Long> {

    @Query("select c from Commissioning c where c.token = :token and c.deleted = false")
    Commissioning findOpenByToken(@Param("token") String token);

    /**
     * Open (not finished) commissionings; there should not be many.
     */
    @Query("select c from Commissioning c where c.deleted = false order by c.createDate")
    List<Commissioning> findAllOpen();
}
