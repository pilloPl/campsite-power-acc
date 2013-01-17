package io.pillopl.persistence.dao.impl;

import io.pillopl.group.attribute.IssuerTenant;
import io.pillopl.group.attribute.SiteTenant;
import io.pillopl.persistence.dao.TenantAttributeDao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public class TenantAttributeDaoImpl implements TenantAttributeDao {

    @Value("#{sqls['findNearestSiteTenantId']}")
    private String findNearestSiteTenantId;

    @Value("#{sqls['findNearestIssuerTenantId']}")
    private String findNearestIssuerTenantId;

    @PersistenceContext
    private EntityManager entityManager;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TenantAttributeDaoImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public SiteTenant findNearestSiteTenant(Long groupId) {
        Long id = findNearestId(findNearestSiteTenantId, groupId);
        return id != null ? entityManager.find(SiteTenant.class, id) : null;
    }

    @Override
    public IssuerTenant findNearestIssuerTenant(Long groupId) {
        Long id = findNearestId(findNearestIssuerTenantId, groupId);
        return id != null ? entityManager.find(IssuerTenant.class, id) : null;
    }

    private Long findNearestId(String sql, Long groupId) {
        Objects.requireNonNull(groupId, "groupId");
        List<Long> ids = jdbcTemplate.queryForList(sql, new MapSqlParameterSource("groupId", groupId), Long.class);
        return ids.isEmpty() ? null : ids.get(0);
    }
}
