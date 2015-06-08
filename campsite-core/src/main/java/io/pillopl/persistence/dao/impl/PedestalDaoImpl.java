package io.pillopl.persistence.dao.impl;

import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalListState;
import io.pillopl.persistence.dao.PedestalDaoCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pedestal SQL from the SQL map plus the state conditions of the pedestal list. The state conditions are fixed
 * fragments chosen by a switch; every value is bound.
 */
public class PedestalDaoImpl implements PedestalDaoCustom {

    private static final String SOCKET_ERROR_STATUSES = "('BREAKER_TRIPPED', 'FAULTED', 'UNKNOWN')";

    @Value("#{sqls['findPedestalIdByCodeRestrictedToUserSiteGroup']}")
    private String findPedestalIdByCodeRestrictedToUserSiteGroup;

    @Value("#{sqls['findPedestalIdsForGuestRestrictedToUserSiteGroup']}")
    private String findPedestalIdsForGuestRestrictedToUserSiteGroup;

    @Value("#{sqls['pedestalsInSiteSubtree']}")
    private String pedestalsInSiteSubtree;

    @PersistenceContext
    private EntityManager entityManager;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PedestalDaoImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Pedestal findByCodeRestrictedToUserSiteGroup(String pedestalCode, Long userSiteGroupId) {
        Objects.requireNonNull(userSiteGroupId, "userSiteGroupId");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userSiteGroupId", userSiteGroupId)
                .addValue("pedestalCode", pedestalCode);
        List<Long> ids = jdbcTemplate.queryForList(findPedestalIdByCodeRestrictedToUserSiteGroup, params, Long.class);
        return ids.isEmpty() ? null : entityManager.find(Pedestal.class, ids.get(0));
    }

    @Override
    public List<Pedestal> findPedestalsForGuestRestrictedToUserSiteGroup(Long userSiteGroupId,
                                                                          Long guestIssuerGroupId) {
        Objects.requireNonNull(userSiteGroupId, "userSiteGroupId");
        Objects.requireNonNull(guestIssuerGroupId, "guestIssuerGroupId");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userSiteGroupId", userSiteGroupId)
                .addValue("guestIssuerGroupId", guestIssuerGroupId);
        return toPedestals(jdbcTemplate.queryForList(findPedestalIdsForGuestRestrictedToUserSiteGroup, params,
                Long.class));
    }

    @Override
    public List<Pedestal> findPedestalsByState(PedestalListState state, Long userSiteGroupId, Integer page,
                                               Integer rowsPerPage) {
        Objects.requireNonNull(userSiteGroupId, "userSiteGroupId");
        StringBuilder sql = new StringBuilder("SELECT sub.ID FROM (")
                .append(pedestalsInSiteSubtree)
                .append(") sub WHERE 1 = 1")
                .append(pedestalForStateCondition(state))
                .append(" ORDER BY sub.CODE ASC");
        MapSqlParameterSource params = new MapSqlParameterSource("userSiteGroupId", userSiteGroupId);
        if (page != null && rowsPerPage != null) {
            sql.append(" LIMIT :limit OFFSET :offset");
            params.addValue("limit", rowsPerPage).addValue("offset", page * rowsPerPage);
        }
        return toPedestals(jdbcTemplate.queryForList(sql.toString(), params, Long.class));
    }

    @Override
    public long countPedestalsByState(PedestalListState state, Long userSiteGroupId) {
        Objects.requireNonNull(userSiteGroupId, "userSiteGroupId");
        String sql = "SELECT COUNT(sub.ID) FROM (" + pedestalsInSiteSubtree + ") sub WHERE 1 = 1"
                + pedestalForStateCondition(state);
        Long count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("userSiteGroupId", userSiteGroupId),
                Long.class);
        return count != null ? count : 0L;
    }

    private String pedestalForStateCondition(PedestalListState state) {
        if (state == null) {
            return "";
        }
        switch (state) {
            case DISABLED:
                return " AND sub.ID IN (SELECT s.PEDESTAL_ID FROM SOCKET s WHERE s.MONITORING = FALSE AND s.DELETED = FALSE)";
            case ERROR:
                return " AND sub.ID IN (SELECT s.PEDESTAL_ID FROM SOCKET s WHERE s.MONITORING = TRUE AND s.DELETED = FALSE"
                        + " AND (s.STATUS IN " + SOCKET_ERROR_STATUSES
                        + " OR EXISTS (SELECT 1 FROM PEDESTAL_STATE ps WHERE ps.PEDESTAL_ID = s.PEDESTAL_ID"
                        + " AND ps.TIMEOUT = TRUE)"
                        + " OR NOT EXISTS (SELECT 1 FROM PEDESTAL_STATE ps WHERE ps.PEDESTAL_ID = s.PEDESTAL_ID)))";
            case WORKING:
                return " AND (sub.ID IN (SELECT s.PEDESTAL_ID FROM SOCKET s WHERE s.MONITORING = TRUE AND s.DELETED = FALSE)"
                        + " AND sub.ID NOT IN (SELECT s.PEDESTAL_ID FROM SOCKET s WHERE s.MONITORING = TRUE"
                        + " AND s.DELETED = FALSE AND s.STATUS IN " + SOCKET_ERROR_STATUSES + ")"
                        + " AND sub.ID IN (SELECT ps.PEDESTAL_ID FROM PEDESTAL_STATE ps WHERE ps.TIMEOUT = FALSE))";
        }
        return "";
    }

    @Override
    public int updatePedestalsDeletedByGroupIds(boolean deleted, boolean listedInApp, List<Long> groupIds,
                                                List<Long> excludeIds) {
        StringBuilder sb = new StringBuilder(
                "update Pedestal p set p.deleted = :deleted, p.listedInApp = :listedInApp where p.group.id in (:groupIds)");
        boolean withExclusions = excludeIds != null && !excludeIds.isEmpty();
        if (withExclusions) {
            sb.append(" and p.id not in (:excludeIds)");
        }
        Query query = entityManager.createQuery(sb.toString());
        query.setParameter("deleted", deleted);
        query.setParameter("listedInApp", listedInApp);
        query.setParameter("groupIds", groupIds);
        if (withExclusions) {
            query.setParameter("excludeIds", excludeIds);
        }
        return query.executeUpdate();
    }

    private List<Pedestal> toPedestals(List<Long> ids) {
        List<Pedestal> pedestals = new ArrayList<>(ids.size());
        for (Long id : ids) {
            pedestals.add(entityManager.find(Pedestal.class, id));
        }
        return pedestals;
    }
}
