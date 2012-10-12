package io.pillopl.persistence.dao;

import io.pillopl.config.SystemProperty;
import io.pillopl.config.SystemPropertyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemPropertyDao extends JpaRepository<SystemProperty, Long> {

    Optional<SystemProperty> findByKey(SystemPropertyKey key);
}
