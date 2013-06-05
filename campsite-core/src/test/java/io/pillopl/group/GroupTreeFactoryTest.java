package io.pillopl.group;

import io.pillopl.dto.group.GroupDTO;
import io.pillopl.dto.group.GroupTreeNodeDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroupTreeFactoryTest {

    private final InMemoryGroups groups = new InMemoryGroups();

    @Test
    void treeFollowsTheChildrenOfTheRoot() {
        Group sites = groups.root(GroupType.SITE, "HR SITES");
        Group bay = groups.child(sites, "BAY CAMPSITE");
        groups.child(bay, "BAY SECTION A");
        groups.child(sites, "COVE CAMPSITE");

        GroupTreeNodeDTO tree = GroupTreeFactory.createTree(sites);

        assertThat(tree.name()).isEqualTo("HR SITES");
        assertThat(tree.children()).extracting(GroupTreeNodeDTO::name).containsExactly("BAY CAMPSITE", "COVE CAMPSITE");
        assertThat(tree.children().get(0).children()).extracting(GroupTreeNodeDTO::name).containsExactly("BAY SECTION A");
        assertThat(GroupTreeFactory.count(tree)).isEqualTo(4);
    }

    @Test
    void childrenOfAnotherTypeAndGroupsMetTwiceAreNotExpanded() {
        Group sites = groups.root(GroupType.SITE, "HR SITES");
        Group bay = groups.child(sites, "BAY CAMPSITE");
        Group wrongType = groups.child(sites, "LOST ISSUER");
        wrongType.setType(GroupType.ISSUER);
        bay.addChild(sites); // broken tree: the root is its own grandchild

        GroupTreeNodeDTO tree = GroupTreeFactory.createTree(sites);

        assertThat(GroupTreeFactory.count(tree)).isEqualTo(2);
        assertThat(GroupTreeFactory.createTree(null)).isNull();
    }

    @Test
    void groupGoesOutWithItsParentId() {
        Group sites = groups.root(GroupType.SITE, "HR SITES");
        Group bay = groups.child(sites, "BAY CAMPSITE");

        List<GroupDTO> dtos = GroupMapper.toDTOs(List.of(sites, bay));

        assertThat(dtos.get(0).parentId()).isNull();
        assertThat(dtos.get(1).parentId()).isEqualTo(sites.getId());
        assertThat(dtos.get(1).type()).isEqualTo("SITE");
    }
}
