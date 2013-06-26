package io.pillopl.persistence.dao;

import io.pillopl.guest.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface GuestDao extends JpaRepository<Guest, Long> {

    @Query("select g from Guest g where g.deleted = false and g.group.id in (:groupIds) order by g.name, g.id")
    List<Guest> findByGroupIds(@Param("groupIds") Collection<Long> groupIds);
}
