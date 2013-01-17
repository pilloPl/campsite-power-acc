package io.pillopl.persistence.dao;

import io.pillopl.group.attribute.LocalListAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface LocalListAttributeDao extends JpaRepository<LocalListAttribute, Long> {

    /**
     * Attributes of the given groups, not in the order of the ids: resolve with GroupAttributeHelper.findNearest.
     */
    @Query("select a from LocalListAttribute a where a.group.id in (:groupIds)")
    List<LocalListAttribute> findByGroupIds(@Param("groupIds") Collection<Long> groupIds);
}
