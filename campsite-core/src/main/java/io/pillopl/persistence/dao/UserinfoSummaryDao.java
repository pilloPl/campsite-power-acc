package io.pillopl.persistence.dao;

import io.pillopl.group.Group;
import io.pillopl.user.UserinfoSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface UserinfoSummaryDao extends JpaRepository<UserinfoSummary, Long> {

    @Query("select s from UserinfoSummary s where s.group in (:groups) "
            + "and (:affiliateOwner is null or s.affiliateOwner = :affiliateOwner) "
            + "and (:seasonalResident is null or s.seasonalResident = :seasonalResident) order by s.username")
    Page<UserinfoSummary> findByGroups(@Param("groups") Collection<Group> groups,
                                       @Param("affiliateOwner") Boolean affiliateOwner,
                                       @Param("seasonalResident") Boolean seasonalResident, Pageable pageable);

    @Modifying
    @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY USERINFO_SUMMARY", nativeQuery = true)
    void refresh();
}
