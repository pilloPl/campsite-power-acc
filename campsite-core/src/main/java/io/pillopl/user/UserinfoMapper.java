package io.pillopl.user;

import io.pillopl.dto.user.CurrentUserDTO;
import io.pillopl.dto.user.UserinfoDTO;
import io.pillopl.dto.user.UserinfoSummaryDTO;
import io.pillopl.group.GroupMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * User entities to transfer objects. The password hash never leaves the services.
 */
public final class UserinfoMapper {

    private UserinfoMapper() {
        throw new AssertionError();
    }

    public static UserinfoDTO toDTO(Userinfo userinfo) {
        if (userinfo == null) {
            return null;
        }
        List<String> roles = new ArrayList<>();
        for (Userrole role : userinfo.getRoles()) {
            roles.add(role.getName());
        }
        return new UserinfoDTO(userinfo.getId(), userinfo.getUsername(), userinfo.getName(), userinfo.getEmail(),
                userinfo.getStatus() != null ? userinfo.getStatus().name() : null, userinfo.getEmailLocale(),
                GroupMapper.toDTO(userinfo.getGroup()), GroupMapper.toDTOs(userinfo.getGroups()), roles,
                userinfo.getCreateDate());
    }

    /**
     * The logged-in user with its effective roles and rights, sorted by name.
     */
    public static CurrentUserDTO toCurrentUserDTO(Userinfo userinfo) {
        java.util.TreeSet<String> roles = new java.util.TreeSet<>();
        java.util.TreeSet<String> rights = new java.util.TreeSet<>();
        for (Userrole role : userinfo.getEffectiveRoles()) {
            roles.add(role.getName());
            for (Userright right : role.getRights()) {
                rights.add(right.getName());
            }
        }
        return new CurrentUserDTO(userinfo.getUsername(), userinfo.getName(), userinfo.getEmail(),
                userinfo.getStatus() != null ? userinfo.getStatus().name() : null, new ArrayList<>(roles),
                new ArrayList<>(rights), GroupMapper.toDTOs(userinfo.getGroups()), userinfo.getLastLoginDate());
    }

    public static List<UserinfoDTO> toDTOs(List<Userinfo> users) {
        List<UserinfoDTO> result = new ArrayList<>(users.size());
        for (Userinfo user : users) {
            result.add(toDTO(user));
        }
        return result;
    }

    public static UserinfoSummaryDTO toDTO(UserinfoSummary summary) {
        return new UserinfoSummaryDTO(summary.getId(), summary.getUsername(), summary.getName(), summary.getEmail(),
                summary.getStatus() != null ? summary.getStatus().name() : null,
                summary.getGroup() != null ? summary.getGroup().getId() : null, summary.isAffiliateOwner(),
                summary.isSeasonalResident());
    }
}
