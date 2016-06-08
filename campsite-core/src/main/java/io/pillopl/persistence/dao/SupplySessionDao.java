package io.pillopl.persistence.dao;

import io.pillopl.infrastructure.Socket;
import io.pillopl.power.PowerSection;
import io.pillopl.supply.SupplySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SupplySessionDao extends JpaRepository<SupplySession, Long> {

    @Query(value = "select nextval('SUPPLY_TRANSACTION_ID_SEQ')", nativeQuery = true)
    Long nextTransactionId();

    /**
     * Active sessions of the socket, the newest first (the id decides between sessions created in the same instant).
     */
    @Query("select s from SupplySession s where s.socket = :socket and s.active = true"
            + " order by s.createDate desc, s.id desc")
    List<SupplySession> findActiveBySocket(@Param("socket") Socket socket);

    @Query("select s from SupplySession s where s.socket.powerSection = :section and s.active = true order by s.id")
    List<SupplySession> findActiveByPowerSection(@Param("section") PowerSection section);

    @Query("select s from SupplySession s where s.socket = :socket and s.card.cardRfid = :rfid"
            + " and s.transactionId = :transactionId")
    SupplySession findBy(@Param("socket") Socket socket, @Param("rfid") String rfid,
                         @Param("transactionId") Integer transactionId);

    @Query("select s from SupplySession s where s.socket = :socket and s.houseCard.rfid = :rfid"
            + " and s.transactionId = :transactionId")
    SupplySession findHouseCardSessionBy(@Param("socket") Socket socket, @Param("rfid") String rfid,
                                         @Param("transactionId") Integer transactionId);

    @Query("select s from SupplySession s where s.socket = :socket and s.qrPayProcessId = :qrPayProcessId"
            + " and s.transactionId = :transactionId")
    SupplySession findQrPaySessionBy(@Param("socket") Socket socket, @Param("qrPayProcessId") Long qrPayProcessId,
                                     @Param("transactionId") Integer transactionId);

    @Query("select s from SupplySession s where s.socket = :socket and s.qrPayProcessId = :qrPayProcessId"
            + " and s.startMeterKwh = :startMeterKwh and s.remoteDate = :remoteDate order by s.id desc")
    List<SupplySession> findQrPaySessionsBy(@Param("socket") Socket socket,
                                            @Param("qrPayProcessId") Long qrPayProcessId,
                                            @Param("startMeterKwh") BigDecimal startMeterKwh,
                                            @Param("remoteDate") LocalDateTime remoteDate);

    SupplySession findFirstByQrPayProcessIdOrderByIdDesc(Long qrPayProcessId);

    @Query("select s from SupplySession s where s.socket = :socket and s.card.cardRfid = :rfid"
            + " and s.startMeterKwh = :startMeterKwh and s.remoteDate = :remoteDate order by s.id desc")
    List<SupplySession> findBy(@Param("socket") Socket socket, @Param("rfid") String rfid,
                               @Param("startMeterKwh") BigDecimal startMeterKwh,
                               @Param("remoteDate") LocalDateTime remoteDate);

    @Query("select s from SupplySession s where s.socket = :socket and s.houseCard.rfid = :rfid"
            + " and s.startMeterKwh = :startMeterKwh and s.remoteDate = :remoteDate order by s.id desc")
    List<SupplySession> findHouseCardSessionsBy(@Param("socket") Socket socket, @Param("rfid") String rfid,
                                                @Param("startMeterKwh") BigDecimal startMeterKwh,
                                                @Param("remoteDate") LocalDateTime remoteDate);

    @Query("select s from SupplySession s where s.socket.pedestal.code = :pedestalCode"
            + " and s.transactionId = :transactionId")
    SupplySession findByPedestalCodeAndTransactionId(@Param("pedestalCode") String pedestalCode,
                                                     @Param("transactionId") Integer transactionId);

    /**
     * Active sessions with no sign of life since the given time: started before it, the socket's last meter reading
     * or status is older, and the pedestal sent no message since (its heartbeat row, when it has one).
     */
    @Query("select s from SupplySession s where s.active = true and coalesce(s.remoteDate, s.createDate) < :silentSince"
            + " and s.socket.modDate < :silentSince"
            + " and not exists (select ps from PedestalState ps where ps.pedestal = s.socket.pedestal"
            + " and ps.approxLastIncoming >= :silentSince)"
            + " order by s.id")
    List<SupplySession> findActiveSilentSince(@Param("silentSince") LocalDateTime silentSince);

    @Query("select s from SupplySession s where s.active = true and s.stayId is not null order by s.id")
    List<SupplySession> findActiveWithStay();
}
