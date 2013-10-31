package io.pillopl.persistence.dao;

import io.pillopl.infrastructure.Pedestal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PedestalDao extends JpaRepository<Pedestal, Long>, PedestalDaoCustom {

    Pedestal findByCode(String code);

    Pedestal findByReferenceId(String referenceId);

    @Query("select p from Pedestal p where p.deleted = false and p.id = :id")
    Pedestal findActiveById(@Param("id") Long id);

    @Query("select p from Pedestal p where p.deleted = false and p.group.id in (:groupIds) order by p.code")
    List<Pedestal> findByGroupIds(@Param("groupIds") Collection<Long> groupIds);

    /**
     * The pedestal when it is not deleted and hangs in one of the groups, otherwise null.
     */
    @Query("select p from Pedestal p where p.deleted = false and p.id = :pedestalId and p.group.id in (:groupIds)")
    Pedestal isInGroupList(@Param("pedestalId") Long pedestalId, @Param("groupIds") Collection<Long> groupIds);

    @Query("select p from Pedestal p where p.deleted = false and p.code = :code and p.group.id in (:groupIds)")
    Pedestal findByCodeAndGroupIds(@Param("code") String code, @Param("groupIds") Collection<Long> groupIds);

    /**
     * Pedestals whose controller keeps a local card list and gets it pushed automatically.
     */
    @Query("select p from Pedestal p where p.deleted = false and p.automatedWhitelistUpdates = true"
            + " and p.pedestalType.localCardList = true order by p.code")
    List<Pedestal> findPedestalsForLocalCardListDistribution();

    @Query("select p from Pedestal p join p.sockets s where s.id = :socketId")
    Pedestal findBySocketId(@Param("socketId") Long socketId);
}
