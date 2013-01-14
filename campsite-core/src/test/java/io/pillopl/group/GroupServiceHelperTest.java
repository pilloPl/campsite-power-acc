package io.pillopl.group;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static io.pillopl.group.InMemoryGroups.agree;
import static org.assertj.core.api.Assertions.assertThat;

class GroupServiceHelperTest {

    private final InMemoryGroups groups = new InMemoryGroups();

    private final Group sites = groups.root(GroupType.SITE, "HR SITES");
    private final Group adriatic = groups.child(sites, "ADRIATIC CAMPSITES");
    private final Group bayCampsite = groups.child(adriatic, "BAY CAMPSITE");
    private final Group bayBeachSection = groups.child(bayCampsite, "BAY BEACH SECTION");
    private final Group pineCampsite = groups.child(adriatic, "PINE CAMPSITE");
    private final Group lakeCampsite = groups.child(sites, "LAKE CAMPSITE");

    private final Group issuers = groups.root(GroupType.ISSUER, "HR ISSUERS");
    private final Group caravanClub = groups.child(issuers, "CARAVAN CLUB");
    private final Group caravanClubJunior = groups.child(caravanClub, "CARAVAN CLUB JUNIOR");
    private final Group caravanClubJuniorSummer = groups.child(caravanClubJunior, "CARAVAN CLUB JUNIOR SUMMER");

    @Test
    void nearestAncestorWithAgreementsReplacesTheListsAboveIt() {
        agree(caravanClub, adriatic);
        agree(caravanClubJunior, lakeCampsite);

        assertThat(GroupServiceHelper.extractAllAcceptingSiteGroupIds(caravanClubJuniorSummer))
                .containsExactly(lakeCampsite.getId());
        assertThat(GroupServiceHelper.extractAllAcceptingSiteGroupIds(caravanClubJunior))
                .containsExactly(lakeCampsite.getId());
    }

    @Test
    void groupWithoutOwnAgreementsInheritsTheNearestAncestorList() {
        agree(caravanClub, bayCampsite, lakeCampsite);

        assertThat(GroupServiceHelper.getAcceptanceAgreements(caravanClubJuniorSummer, GroupType.SITE))
                .containsExactly(bayCampsite, lakeCampsite);
    }

    @Test
    void agreementCoversTheWholeSubtreeOfTheAgreedSiteGroup() {
        agree(caravanClub, adriatic);

        assertThat(GroupServiceHelper.extractAllAcceptingSiteGroupIds(caravanClub))
                .containsExactlyInAnyOrder(adriatic.getId(), bayCampsite.getId(), bayBeachSection.getId(),
                        pineCampsite.getId());
    }

    @Test
    void overlappingAgreementsListEveryAcceptingGroupOnce() {
        agree(caravanClub, adriatic, bayCampsite);

        assertThat(GroupServiceHelper.extractAllAcceptingSiteGroupIds(caravanClub))
                .containsExactlyInAnyOrder(adriatic.getId(), bayCampsite.getId(), bayBeachSection.getId(),
                        pineCampsite.getId());
    }

    @Test
    void noAgreementOnTheWholeParentLineMeansNoAcceptingGroups() {
        assertThat(GroupServiceHelper.extractAllAcceptingSiteGroupIds(caravanClubJuniorSummer)).isEmpty();
        assertThat(GroupServiceHelper.extractAllAcceptingSiteGroupIds(null)).isEmpty();
    }

    @Test
    void associatedGroupsOfAnotherTypeAreNotAgreementsWhereverTheyStandInTheList() {
        Group staff = groups.root(GroupType.STAFF, "HR STAFF");
        agree(caravanClub, lakeCampsite);
        caravanClubJunior.addAssociatedGroup(staff);
        caravanClubJunior.addAssociatedGroup(pineCampsite);

        assertThat(GroupServiceHelper.getAcceptanceAgreements(caravanClubJunior, GroupType.SITE))
                .containsExactly(pineCampsite);
    }

    @Test
    void listWithOnlyGroupsOfAnotherTypeFallsThroughToTheParent() {
        Group staff = groups.root(GroupType.STAFF, "HR STAFF");
        agree(caravanClub, lakeCampsite);
        caravanClubJunior.addAssociatedGroup(staff);

        assertThat(GroupServiceHelper.getAcceptanceAgreements(caravanClubJunior, GroupType.SITE))
                .containsExactly(lakeCampsite);
    }

    @Test
    void subtreeIdsCanBeFilteredByDisplayNameIgnoringCase() {
        assertThat(GroupServiceHelper.extractAllManageableGroupIDsForGroup(adriatic, "campsite"))
                .containsExactlyInAnyOrder(adriatic.getId(), bayCampsite.getId(), pineCampsite.getId());
        assertThat(GroupServiceHelper.extractAllManageableGroupIDsForGroup(adriatic, null))
                .hasSize(4);
    }

    @Test
    void subtreeStartsWithTheGroupItself() {
        assertThat(GroupServiceHelper.extractAllManageableGroupsForGroup(bayCampsite))
                .containsExactly(bayCampsite, bayBeachSection);
        assertThat(GroupServiceHelper.extractAllManageableGroupsForGroup(null)).isEmpty();
    }

    @Test
    void marketIsReadFromTheNamePrefixFollowedByASpace() {
        assertThat(GroupServiceHelper.checkMarket(sites)).isEqualTo(Market.HR);
        assertThat(GroupServiceHelper.checkMarket(groups.root(GroupType.SITE, "IT SITES"))).isEqualTo(Market.IT);
        assertThat(GroupServiceHelper.checkMarket(groups.root(GroupType.SITE, "FRANCE"))).isNull();
        assertThat(GroupServiceHelper.checkMarket(groups.root(GroupType.SITE, "UNKNOWN SITES"))).isNull();
        assertThat(GroupServiceHelper.checkMarket(adriatic)).isNull();
        assertThat(GroupServiceHelper.checkMarket(null)).isNull();
    }

    @Test
    void childOfAnotherTypeThanItsParentBreaksTheTypeHierarchy() {
        Group wrong = groups.child(bayCampsite, "WRONG");
        wrong.setType(GroupType.STAFF);
        List<String> errors = new ArrayList<>();

        GroupServiceHelper.checkParent(wrong, errors, new HashSet<>());
        GroupServiceHelper.checkParent(pineCampsite, errors, new HashSet<>());

        assertThat(errors).singleElement().asString().contains("Type hierarchy broken", "WRONG");
    }

    @Test
    void onlyIssuerToSiteAgreementsAreValid() {
        List<String> errors = new ArrayList<>();
        agree(caravanClub, adriatic);
        bayCampsite.addAssociatedGroup(lakeCampsite);
        caravanClubJunior.addAssociatedGroup(issuers);

        GroupServiceHelper.checkAgreements(caravanClub, errors);
        GroupServiceHelper.checkAgreements(bayCampsite, errors);
        GroupServiceHelper.checkAgreements(caravanClubJunior, errors);

        assertThat(errors).hasSize(2);
    }

    @Test
    void groupMarketsAreOnlyThePrefixMarkets() {
        assertThat(GroupServiceHelper.getGroupMarkets()).containsExactly("HR", "IT", "FR");
    }

    @Test
    void groupsAreComparedByNameWithNullsFirst() {
        GroupServiceHelper.GroupNameComparator comparator = new GroupServiceHelper.GroupNameComparator();

        assertThat(comparator.compare(bayCampsite, pineCampsite)).isNegative();
        assertThat(comparator.compare(null, pineCampsite)).isNegative();
        assertThat(comparator.compare(pineCampsite, null)).isPositive();
        assertThat(comparator.compare(null, null)).isZero();
    }
}
