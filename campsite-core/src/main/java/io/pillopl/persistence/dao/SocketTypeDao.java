package io.pillopl.persistence.dao;

import io.pillopl.infrastructure.SocketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SocketTypeDao extends JpaRepository<SocketType, Long> {

    SocketType findByName(String name);

    @Query("select t from SocketType t where t.deleted = false order by t.id")
    List<SocketType> findAllActive();
}
