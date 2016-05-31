package io.pillopl.persistence.dao.impl;

import io.pillopl.group.Group;
import io.pillopl.persistence.dao.GroupDaoCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Recursive group SQL from the SQL map (PostgreSQL {@code WITH RECURSIVE}), parameters always bound.
 * Entity variants read the ordered ids first and then the entities, keeping the hierarchical order.
 */
public class GroupDaoImpl implements GroupDaoCustom {

    @Value("#{sqls['findSubtreeGroupIds']}")
    private String findSubtreeGroupIds;

    @Value("#{sqls['findParentGroupIds']}")
    private String findParentGroupIds;

    @Value("#{sqls['findAcceptingGroupIds']}")
    private String findAcceptingGroupIds;

    @Value("#{sqls['findGroupIdsAcceptedAt']}")
    private String findGroupIdsAcceptedAt;

    @PersistenceContext
    private EntityManager entityManager;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GroupDaoImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Long> findManageable(Long groupId) {
        Objects.requireNonNull(groupId, "groupId");
        return jdbcTemplate.queryForList(findSubtreeGroupIds,
                new MapSqlParameterSource("groupId", groupId), Long.class);
    }

    @Override
    public List<Group> findManageableGroups(Long groupId) {
        return toGroups(findManageable(groupId));
    }

    @Override
    public List<Long> findParents(Long groupId) {
        Objects.requireNonNull(groupId, "groupId");
        return jdbcTemplate.queryForList(findParentGroupIds,
                new MapSqlParameterSource("groupId", groupId), Long.class);
    }

    @Override
    public List<Group> findParentGroups(Long groupId) {
        return toGroups(findParents(groupId));
    }

    @Override
    public List<Long> findAcceptingGroupIds(Long groupId, String targetType) {
        Objects.requireNonNull(groupId, "groupId");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("groupId", groupId)
                .addValue("targetType", targetType);
        return jdbcTemplate.queryForList(findAcceptingGroupIds, params, Long.class);
    }

    @Override
    public List<Long> findGroupIdsAcceptedAt(Long groupId, String sourceType, String targetType) {
        Objects.requireNonNull(groupId, "groupId");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("groupId", groupId)
                .addValue("sourceType", sourceType)
                .addValue("targetType", targetType);
        return jdbcTemplate.queryForList(findGroupIdsAcceptedAt, params, Long.class);
    }

    private List<Group> toGroups(List<Long> ids) {
        List<Group> groups = new ArrayList<>(ids.size());
        for (Long id : ids) {
            groups.add(entityManager.find(Group.class, id));
        }
        return groups;
    }
}
