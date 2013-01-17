package io.pillopl.group.attribute;

import io.pillopl.group.Group;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;
import lombok.Getter;
import lombok.Setter;

/**
 * Setting attached to a group and inherited by its subtree. Every attribute has its own table (GRP_ATTR_*),
 * all ids come from one sequence.
 * <p>
 * Resolution is the same for every attribute: walk the parent line from the group up, the attribute of the nearest
 * ancestor-or-self wins (see {@link GroupAttributeHelper#findNearest}).
 */
@MappedSuperclass
@Getter
@Setter
public abstract class GroupAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_group_attribute")
    @SequenceGenerator(name = "seq_group_attribute", sequenceName = "GROUP_ATTRIBUTE_ID_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "GROUP_ID", nullable = false)
    protected Group group;

    public Long getGroupId() {
        return group != null ? group.getId() : null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", groupId=" + getGroupId() + "}";
    }
}
