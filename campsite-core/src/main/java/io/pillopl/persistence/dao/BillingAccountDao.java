package io.pillopl.persistence.dao;

import io.pillopl.guest.BillingAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingAccountDao extends JpaRepository<BillingAccount, Long> {

    BillingAccount findByErpAccountNo(String erpAccountNo);

    List<BillingAccount> findByGuestIdOrderById(Long guestId);

    /**
     * The accounts the ERP knows (the monthly ERP billing).
     */
    List<BillingAccount> findByErpAccountNoIsNotNullOrderById();
}
