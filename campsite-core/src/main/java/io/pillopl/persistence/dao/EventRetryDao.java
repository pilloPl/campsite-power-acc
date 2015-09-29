package io.pillopl.persistence.dao;

import io.pillopl.event.EventRetry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Events waiting to be sent again, oldest first.
 */
public interface EventRetryDao extends JpaRepository<EventRetry, Long> {

    List<EventRetry> findByStatusOrderByIdAsc(EventRetry.Status status);
}
