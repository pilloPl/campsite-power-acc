package io.pillopl.webservice.console.group;

import io.pillopl.dto.group.GroupTreeNodeDTO;

import java.io.Serializable;

/**
 * The scope group with its subtree.
 */
public record GetGroupTreeResponse(GroupTreeNodeDTO tree) implements Serializable {
}
