package io.pillopl.dto.group;

import java.io.Serializable;

/**
 * A group of one of the three trees (ISSUER, SITE, STAFF) without its relations.
 */
public record GroupDTO(Long id, String name, String displayName, String type, Long parentId) implements Serializable {
}
