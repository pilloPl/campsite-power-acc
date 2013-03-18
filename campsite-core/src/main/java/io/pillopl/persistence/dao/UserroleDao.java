package io.pillopl.persistence.dao;

import io.pillopl.user.Userrole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserroleDao extends JpaRepository<Userrole, Long> {

    Userrole findByName(String name);

    @Query("select r from Userrole r where r.selfService = true order by r.sortKey")
    List<Userrole> findAllSelfServiceRoles();

    @Query("select r from Userrole r order by r.sortKey")
    List<Userrole> findAllOrdered();
}
