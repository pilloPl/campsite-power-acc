package io.pillopl.persistence.dao;

import io.pillopl.event.EventRetry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Events waiting to be sent again, oldest first.
 */
public interface EventRetryDao extends JpaRepository<EventRetry, Long> {

    List<EventRetry> findByStatusOrderByIdAsc(EventRetry.Status status);

    List<EventRetry> findByStatusOrderByIdDesc(EventRetry.Status status,
                                               org.springframework.data.domain.Pageable pageable);
}
