package io.pillopl.persistence.dao;

import io.pillopl.group.attribute.CommunicationLogAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommunicationLogAttributeDao extends JpaRepository<CommunicationLogAttribute, Long> {

    /**
     * Attributes of the given groups, not in the order of the ids: resolve with GroupAttributeHelper.findNearest.
     */
    @Query("select a from CommunicationLogAttribute a where a.group.id in (:groupIds)")
    List<CommunicationLogAttribute> findByGroupIds(@Param("groupIds") Collection<Long> groupIds);
}
