package io.pillopl.persistence.dao;

import io.pillopl.order.Order;
import io.pillopl.order.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface OrderDao extends JpaRepository<Order, Long> {

    Order findByOrderNumber(String orderNumber);

    List<Order> findByGuestIdOrderByCreateDateDesc(Long guestId);

    List<Order> findByStatusOrderByCreateDate(OrderStatus status);

    /**
     * Orders waiting for the ERP to create partner and account, optionally only those not handed to the ERP yet.
     */
    @Query("select o from OrderHeader o where o.status = io.pillopl.order.OrderStatus.AWAITING_ERP_ACCOUNT"
            + " and (:notExportedOnly = false or o.exported = false)"
            + " and (o.erpPartnerNo is null or trim(o.erpPartnerNo) = '' or o.erpAccountNo is null"
            + " or trim(o.erpAccountNo) = '') order by o.createDate, o.id")
    List<Order> findOrdersAwaitingErpAccount(@Param("notExportedOnly") boolean notExportedOnly);

    @Modifying
    @Query("update OrderHeader o set o.exported = true, o.exportDate = :at where o.id in (:orderIds)")
    int updateOrdersSetExported(@Param("orderIds") Collection<Long> orderIds, @Param("at") LocalDateTime at);

    @Query(value = "SELECT nextval('ORDER_NUMBER_SEQ')", nativeQuery = true)
    Long nextOrderNumber();

    /**
     * The ERP corrected the numbers of a partner and account: orders, guest, billing account and cards follow
     * (ORDER_UTILS.UPDATE_ERP_NUMBERS).
     *
     * @return the number of orders updated
     */
    @Query(value = "SELECT ORDER_UTILS.UPDATE_ERP_NUMBERS(:oldPartnerNo, :oldAccountNo, :newPartnerNo, :newAccountNo)",
            nativeQuery = true)
    Integer updateErpNumbers(@Param("oldPartnerNo") String oldPartnerNo, @Param("oldAccountNo") String oldAccountNo,
                             @Param("newPartnerNo") String newPartnerNo, @Param("newAccountNo") String newAccountNo);
}
