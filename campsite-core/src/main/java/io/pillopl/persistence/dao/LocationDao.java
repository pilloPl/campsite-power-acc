package io.pillopl.persistence.dao;

import io.pillopl.infrastructure.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LocationDao extends JpaRepository<Location, Long> {

    Location findFirstByNameAndDeletedFalseOrderById(String name);

    @Query("select l from Location l where l.deleted = false and upper(l.street) = upper(:street) "
            + "and upper(l.houseNumber) = upper(:houseNumber) and l.zipcode = :zipcode and upper(l.city) = upper(:city) "
            + "order by l.id")
    List<Location> findByAddress(@Param("street") String street, @Param("houseNumber") String houseNumber,
                                 @Param("zipcode") String zipcode, @Param("city") String city);
}
