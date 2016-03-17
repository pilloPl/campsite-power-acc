package io.pillopl.persistence.dao.impl;

import io.pillopl.card.CampingCard;
import io.pillopl.issuer.PartnerSyncState;
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

    @Value("#{sqls['findCardIdsValidForPartners']}")
    private String findCardIdsValidForPartners;

    @Value("#{sqls['findCardIdsExpiredForPartners']}")
    private String findCardIdsExpiredForPartners;

    @Value("#{sqls['findCardIdsForPartnerExport']}")
    private String findCardIdsForPartnerExport;

    private static final String UPSERT_CARD = "SELECT CARD_UTILS.UPSERT_CARD(:cardRfidHex, :cardNumber, :validFrom,"
            + " :validTo, :billingAccountNo, :guestId)";

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

    @Override
    public Long upsertCard(String cardRfidHex, String cardNumber, LocalDate validFrom, LocalDate validTo,
                           String billingAccountNo, Long guestId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("cardRfidHex", cardRfidHex, java.sql.Types.VARCHAR)
                .addValue("cardNumber", cardNumber, java.sql.Types.VARCHAR)
                .addValue("validFrom", validFrom, java.sql.Types.DATE)
                .addValue("validTo", validTo, java.sql.Types.DATE)
                .addValue("billingAccountNo", billingAccountNo, java.sql.Types.VARCHAR)
                .addValue("guestId", guestId, java.sql.Types.BIGINT);
        // the card rows changed behind the persistence context
        entityManager.flush();
        Long id = jdbcTemplate.queryForObject(UPSERT_CARD, params, Long.class);
        entityManager.clear();
        return id;
    }

    @Override
    public int updateExpiredCardsPartnerSyncState(LocalDate today) {
        Objects.requireNonNull(today, "today");
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("today", today);

        // the stay or season runs: the partners shall have the card
        List<CampingCard> cards = toCards(jdbcTemplate.queryForList(findCardIdsValidForPartners, params, Long.class));
        for (CampingCard card : cards) {
            card.calculatePartnerSyncState(PartnerSyncState.ADD);
        }
        int count = cards.size();

        // the stay or season is over: the partners shall delete the card
        cards = toCards(jdbcTemplate.queryForList(findCardIdsExpiredForPartners, params, Long.class));
        for (CampingCard card : cards) {
            card.calculatePartnerSyncState(PartnerSyncState.DELETE);
        }
        entityManager.flush();
        return count + cards.size();
    }

    @Override
    public List<CampingCard> findCardsForPartnerExport(Long issuerGroupId) {
        if (issuerGroupId == null) {
            return new ArrayList<>();
        }
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("issuerGroupId", issuerGroupId);
        return toCards(jdbcTemplate.queryForList(findCardIdsForPartnerExport, params, Long.class));
    }

    private List<CampingCard> toCards(List<Long> ids) {
        List<CampingCard> cards = new ArrayList<>(ids.size());
        for (Long id : ids) {
            cards.add(entityManager.find(CampingCard.class, id));
        }
        return cards;
    }
}
