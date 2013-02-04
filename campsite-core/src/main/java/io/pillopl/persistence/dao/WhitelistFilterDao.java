package io.pillopl.persistence.dao;

import io.pillopl.group.attribute.WhitelistFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface WhitelistFilterDao extends JpaRepository<WhitelistFilter, Long> {

    /**
     * Attributes of the given groups, not in the order of the ids: resolve with GroupAttributeHelper.findNearest.
     */
    @Query("select a from WhitelistFilter a where a.group.id in (:groupIds)")
    List<WhitelistFilter> findByGroupIds(@Param("groupIds") Collection<Long> groupIds);
}
