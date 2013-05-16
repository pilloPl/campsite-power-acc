package io.pillopl.persistence.dao;

import io.pillopl.infrastructure.SocketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SocketTypeDao extends JpaRepository<SocketType, Long> {

    SocketType findByName(String name);

    @Query("select t from SocketType t where t.deleted = false and t.plugStandard = :plug and t.phases = :phases "
            + "and t.breakerRating.rating = :rating order by t.id")
    List<SocketType> findByTechnicalData(@Param("plug") io.pillopl.infrastructure.PlugStandard plug,
                                         @Param("phases") int phases,
                                         @Param("rating") io.pillopl.infrastructure.BreakerRating rating);

    @Query("select t from SocketType t where t.deleted = false order by t.id")
    List<SocketType> findAllActive();
}
