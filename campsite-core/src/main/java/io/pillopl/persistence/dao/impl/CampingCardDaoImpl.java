package io.pillopl.persistence.dao.impl;

import io.pillopl.card.CampingCard;
import io.pillopl.persistence.dao.CampingCardDaoCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Camping card SQL from the SQL map; every value is bound.
 */
public class CampingCardDaoImpl implements CampingCardDaoCustom {

    @Value("#{sqls['findValidCardsForLocalCardList']}")
    private String findValidCardsForLocalCardList;

    @PersistenceContext
    private EntityManager entityManager;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CampingCardDaoImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<CampingCard> findValidCardsForLocalCardList(Collection<Long> issuerGroupIds, LocalDate day) {
        Objects.requireNonNull(day, "day");
        if (issuerGroupIds == null || issuerGroupIds.isEmpty()) {
            return new ArrayList<>();
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("issuerGroupIds", issuerGroupIds)
                .addValue("day", day);
        return toCards(jdbcTemplate.queryForList(findValidCardsForLocalCardList, params, Long.class));
    }

    private List<CampingCard> toCards(List<Long> ids) {
        List<CampingCard> cards = new ArrayList<>(ids.size());
        for (Long id : ids) {
            cards.add(entityManager.find(CampingCard.class, id));
        }
        return cards;
    }
}
