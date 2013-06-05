package io.pillopl.group;

import io.pillopl.dto.group.GroupDTO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Group entity to transfer object.
 */
public final class GroupMapper {

    private GroupMapper() {
        throw new AssertionError();
    }

    public static GroupDTO toDTO(Group group) {
        if (group == null) {
            return null;
        }
        Long parentId = group.getParent() != null ? group.getParent().getId() : null;
        return new GroupDTO(group.getId(), group.getName(), group.getDisplayName(), group.getType(), parentId);
    }

    public static List<GroupDTO> toDTOs(Collection<Group> groups) {
        List<GroupDTO> result = new ArrayList<>();
        if (groups != null) {
            for (Group group : groups) {
                result.add(toDTO(group));
            }
        }
        return result;
    }
}
