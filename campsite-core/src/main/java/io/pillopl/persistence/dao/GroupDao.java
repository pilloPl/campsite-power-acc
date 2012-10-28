package io.pillopl.persistence.dao;

import io.pillopl.group.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupDao extends JpaRepository<Group, Long>, GroupDaoCustom {

    Group findByNameAndType(String name, String type);

    @Query("select g from Group g where upper(g.name) = upper(:name) and g.type = :type")
    Group findByNotCaseSensitiveNameAndType(@Param("name") String name, @Param("type") String type);

    @Query("select g from Group g where g.type = :type and g.parent is null order by g.id")
    List<Group> findRoots(@Param("type") String type);
}
