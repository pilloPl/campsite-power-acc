package io.pillopl.group;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Node of one of the three typed organisation trees (see {@link GroupType}).
 * <p>
 * The name carries logic (market prefix, affiliate prefix, roots looked up by name) and is unique within the type.
 */
@Entity
@Table(name = "TBL_GROUP")
@Getter
@Setter
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_group")
    @SequenceGenerator(name = "seq_group", sequenceName = "GROUP_ID_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID")
    private Group parent;

    @OneToMany(mappedBy = "parent")
    @OrderBy("id")
    private List<Group> children = new ArrayList<>();

    @Column(name = "TYPENAME", nullable = false)
    private String type;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DISPLAYNAME", nullable = false, length = 256)
    private String displayName;

    public Group() {
    }

    public Group(GroupType type, String name) {
        this(type, name, name);
    }

    public Group(GroupType type, String name, String displayName) {
        this.type = type.name();
        this.name = name;
        this.displayName = displayName;
    }

    public void setType(GroupType type) {
        this.type = type.name();
    }

    public void setType(String type) {
        this.type = type;
    }

    public void addChild(Group group) {
        if (!children.contains(group)) {
            children.add(group);
        }
    }

    @Override
    public String toString() {
        return "Group{id=" + id + ", type='" + type + "', name='" + name + "', displayName='" + displayName + "'}";
    }
}
