package io.pillopl.persistence.dao;

import io.pillopl.group.Group;
import io.pillopl.user.Userinfo;
import io.pillopl.user.Userrole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserinfoDao extends JpaRepository<Userinfo, Long> {

    Userinfo findByUsername(String username);

    @Query("select u from Userinfo u where lower(u.username) like lower(concat(:username, '%')) order by u.username")
    List<Userinfo> findByUsernameStartingWith(@Param("username") String username);

    /**
     * All users with this username ignoring case (deleted ones too): a generated username must not collide.
     */
    @Query("select u from Userinfo u where lower(u.username) = lower(:username)")
    List<Userinfo> findAllByUsernameIgnoreCase(@Param("username") String username);

    @Query("select u from Userinfo u where lower(u.email) = lower(:email) and u.deleted = false order by u.id")
    List<Userinfo> findByEmail(@Param("email") String email);

    /**
     * Users having the group in their scope groups.
     */
    @Query("select u from Userinfo u join u.groups g where g = :group and u.deleted = false order by u.id")
    List<Userinfo> findByAssociatedGroup(@Param("group") Group group);

    @Query("select distinct u from Userinfo u join u.roles r where u.group in (:groups) and r in (:roles) "
            + "and u.deleted = false order by u.username")
    List<Userinfo> findByGroupsAndUserroles(@Param("groups") Collection<Group> groups,
                                            @Param("roles") Collection<Userrole> roles);

    @Query(value = "select distinct u from Userinfo u join u.roles r where u.group in (:groups) and r in (:roles) "
            + "and u.deleted = false and (lower(u.username) like lower(concat('%', :search, '%')) "
            + "or lower(u.name) like lower(concat('%', :search, '%')) "
            + "or lower(u.email) like lower(concat('%', :search, '%')))",
            countQuery = "select count(distinct u) from Userinfo u join u.roles r where u.group in (:groups) "
                    + "and r in (:roles) and u.deleted = false and (lower(u.username) like lower(concat('%', :search, '%')) "
                    + "or lower(u.name) like lower(concat('%', :search, '%')) "
                    + "or lower(u.email) like lower(concat('%', :search, '%')))")
    Page<Userinfo> findByGroupsAndSearchAndUserroles(@Param("groups") Collection<Group> groups,
                                                     @Param("roles") Collection<Userrole> roles,
                                                     @Param("search") String search, Pageable pageable);
}
