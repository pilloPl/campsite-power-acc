package io.pillopl.dto.group;

import java.io.Serializable;
import java.util.List;

/**
 * A group with its subtree, for the tree diagrams of the console.
 */
public record GroupTreeNodeDTO(Long id, String name, String displayName, String type,
                               List<GroupTreeNodeDTO> children) implements Serializable {

    public GroupTreeNodeDTO {
        children = children != null ? List.copyOf(children) : List.of();
    }
}
