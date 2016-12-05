package io.pillopl.persistence.dao;

import io.pillopl.monitoring.ConsoleAlertAck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ConsoleAlertAckDao extends JpaRepository<ConsoleAlertAck, Long> {

    List<ConsoleAlertAck> findByAlertKeyIn(Collection<String> alertKeys);

    ConsoleAlertAck findByAlertKey(String alertKey);
}
