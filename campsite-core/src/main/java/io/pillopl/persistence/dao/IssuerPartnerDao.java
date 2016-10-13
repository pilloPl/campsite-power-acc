package io.pillopl.persistence.dao;

import io.pillopl.issuer.IssuerPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IssuerPartnerDao extends JpaRepository<IssuerPartner, Long> {

    IssuerPartner findByName(String name);

    /**
     * Partners the exchange job works with, in a stable order.
     */
    @Query("select p from IssuerPartner p where p.deleted = false and p.active = true order by p.name")
    List<IssuerPartner> findExchanging();

    List<IssuerPartner> findByDeletedFalseOrderByName();
}
