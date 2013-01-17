package io.pillopl.persistence.dao;

import io.pillopl.group.attribute.SiteTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SiteTenantDao extends JpaRepository<SiteTenant, Long> {

    /**
     * Attributes of the given groups, not in the order of the ids: resolve with GroupAttributeHelper.findNearest.
     */
    @Query("select a from SiteTenant a where a.group.id in (:groupIds)")
    List<SiteTenant> findByGroupIds(@Param("groupIds") Collection<Long> groupIds);
}
