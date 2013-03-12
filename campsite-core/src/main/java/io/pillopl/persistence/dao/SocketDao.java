package io.pillopl.persistence.dao;

import io.pillopl.infrastructure.Socket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SocketDao extends JpaRepository<Socket, Long> {

    Socket findBySocketCode(String socketCode);

    @Query("select s from Socket s where s.deleted = false and s.pedestal.code = :pedestalCode and s.socketNo = :socketNo")
    Socket findByPedestalCodeAndSocketNo(@Param("pedestalCode") String pedestalCode,
                                            @Param("socketNo") Integer socketNo);

    @Query("select s from Socket s where s.deleted = false and s.pedestal.id = :pedestalId order by s.position")
    List<Socket> findActiveByPedestalId(@Param("pedestalId") Long pedestalId);

    @Query("select s from Socket s where s.deleted = false and s.pitchNo = :pitchNo and s.pedestal.group.id = :groupId")
    List<Socket> findByPitchNoInGroup(@Param("pitchNo") String pitchNo, @Param("groupId") Long groupId);
}
