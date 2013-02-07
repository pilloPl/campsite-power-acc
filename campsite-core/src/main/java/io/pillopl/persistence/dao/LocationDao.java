package io.pillopl.persistence.dao;

import io.pillopl.infrastructure.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationDao extends JpaRepository<Location, Long> {

    Location findFirstByNameAndDeletedFalseOrderById(String name);
}
