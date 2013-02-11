package io.pillopl.persistence.dao;

import io.pillopl.infrastructure.BreakerRating;
import io.pillopl.infrastructure.BreakerRatingSpecification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BreakerRatingSpecificationDao extends JpaRepository<BreakerRatingSpecification, Long> {

    BreakerRatingSpecification findByRating(BreakerRating rating);
}
