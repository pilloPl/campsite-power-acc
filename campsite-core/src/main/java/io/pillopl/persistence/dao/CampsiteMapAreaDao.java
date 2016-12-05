package io.pillopl.persistence.dao;

import io.pillopl.infrastructure.CampsiteMapArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CampsiteMapAreaDao extends JpaRepository<CampsiteMapArea, Long> {

    List<CampsiteMapArea> findBySiteGroupIdInOrderBySiteGroupIdAscAreaTypeAscNameAsc(Collection<Long> siteGroupIds);
}
