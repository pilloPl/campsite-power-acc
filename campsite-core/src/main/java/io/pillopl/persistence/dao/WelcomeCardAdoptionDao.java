package io.pillopl.persistence.dao;

import io.pillopl.group.attribute.WelcomeCardAdoption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface WelcomeCardAdoptionDao extends JpaRepository<WelcomeCardAdoption, Long> {

    /**
     * Attributes of the given groups, not in the order of the ids: resolve with GroupAttributeHelper.findNearest.
     */
    @Query("select a from WelcomeCardAdoption a where a.group.id in (:groupIds)")
    List<WelcomeCardAdoption> findByGroupIds(@Param("groupIds") Collection<Long> groupIds);

    @Query("select a from WelcomeCardAdoption a where a.group.id = :groupId")
    WelcomeCardAdoption findForGroup(@Param("groupId") Long groupId);
}
