package io.pillopl.card;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.group.attribute.LocalListAttribute;
import io.pillopl.group.attribute.LocalListAttributeService;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalLinkVersion;
import io.pillopl.infrastructure.PedestalType;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.LocalListVersionDao;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.util.DateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalCardListServiceTest {

    static final String CODE = "HR-BAY-P001";

    final GroupService groupService = mock(GroupService.class);
    final PedestalDao pedestalDao = mock(PedestalDao.class);
    final LocalListAttributeService localListAttributeService = mock(LocalListAttributeService.class);
    final CampingCardDao campingCardDao = mock(CampingCardDao.class);
    final LocalListVersionDao localListVersionDao = mock(LocalListVersionDao.class);
    final HouseCardService houseCardService = mock(HouseCardService.class);
    final LocalCardListSender sender = mock(LocalCardListSender.class);
    final SystemPropertyService systemPropertyService = mock(SystemPropertyService.class);
    final LocalCardListService service = new LocalCardListService(groupService, pedestalDao,
            localListAttributeService, campingCardDao, localListVersionDao, houseCardService, sender,
            systemPropertyService, new DateService());

    final PedestalType type = new PedestalType("ACME", "P4", PedestalLinkVersion.V1);
    final Pedestal pedestal = new Pedestal(CODE, type, null);

    @BeforeEach
    void setUp() {
        pedestal.setId(1L);
        pedestal.setGroup(new Group(GroupType.SITE, "BAY CAMPSITE"));
        type.setLocalCardList(true);
        type.setLocalCardListCapacity(3);
        when(pedestalDao.findByCode(CODE)).thenReturn(pedestal);
        when(systemPropertyService.getLocalCardListDefaultMaxEntries()).thenReturn(100);
        when(localListVersionDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sender.sendLocalList(any(), anyBoolean(), anyList(), anyInt(), anyBoolean()))
                .thenReturn(LocalCardListSendResult.ok());
    }

    @Test
    void pedestalWithoutLocalListAttributeGetsNoList() {
        assertThat(service.sendFull(CODE, false)).isFalse();
        verify(sender, never()).sendLocalList(any(), anyBoolean(), anyList(), anyInt(), anyBoolean());
    }

    @Test
    void controllerWithoutLocalListGetsNoList() {
        attribute(true, true);
        type.setLocalCardList(false);

        assertThat(service.sendFull(CODE, false)).isFalse();
        verify(sender, never()).sendLocalList(any(), anyBoolean(), anyList(), anyInt(), anyBoolean());
    }

    @Test
    void houseCardsComeFirstThenTheNewestCardsUpToTheCapacity() {
        attribute(true, true);
        houseCards(houseCard("H1", null));
        cards("C1", "C2", "C3");

        LocalCardList list = service.buildLocalCardList(CODE);

        assertThat(list.entries()).extracting(LocalCardListEntry::rfid).containsExactly("H1", "C1", "C2");
        assertThat(list.entries().get(0).houseCard()).isTrue();
        assertThat(list.droppedEntries()).isEqualTo(1);
        assertThat(list.capacity()).isEqualTo(3);
    }

    @Test
    void tooManyHouseCardsPushAllCardsOutAndTheLastUsedStay() {
        attribute(true, true);
        houseCards(houseCard("H1", null), houseCard("H2", LocalDateTime.of(2026, 7, 1, 10, 0)),
                houseCard("H3", LocalDateTime.of(2026, 7, 9, 10, 0)), houseCard("H4", LocalDateTime.of(2026, 7, 5, 10, 0)));
        cards("C1");

        LocalCardList list = service.buildLocalCardList(CODE);

        assertThat(list.entries()).extracting(LocalCardListEntry::rfid).containsExactly("H3", "H4", "H2");
        assertThat(list.cards()).isZero();
        assertThat(list.droppedEntries()).isEqualTo(2);
    }

    @Test
    void attributeDecidesWhichKindsOfCardsAreListed() {
        attribute(false, true);
        houseCards(houseCard("H1", null));
        cards("C1");

        assertThat(service.buildLocalCardList(CODE).entries()).extracting(LocalCardListEntry::rfid)
                .containsExactly("H1");
    }

    @Test
    void typeWithoutConfiguredCapacityUsesTheDefaultFromTheConfiguration() {
        type.setLocalCardListCapacity(null);
        when(systemPropertyService.getLocalCardListDefaultMaxEntries()).thenReturn(1);
        attribute(true, false);
        cards("C1", "C2");

        assertThat(service.buildLocalCardList(CODE).entries()).hasSize(1);
    }

    @Test
    void fullListIsSentWithTheNextVersion() {
        attribute(true, true);
        cards("C1");
        LocalListVersion version = new LocalListVersion(pedestal, 6);
        when(localListVersionDao.findByPedestal(pedestal)).thenReturn(version);

        assertThat(service.sendFull(CODE, true)).isTrue();

        verify(sender).sendLocalList(eq(pedestal), eq(true), anyList(), eq(7), eq(true));
    }

    @Test
    void firstListOfAPedestalHasVersionOne() {
        assertThat(service.getNextLocalListVersion(pedestal)).isEqualTo(1);
        assertThatThrownBy(() -> service.getNextLocalListVersion(new Pedestal()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void failedSendIsReported() {
        attribute(true, true);
        when(sender.sendLocalList(any(), anyBoolean(), anyList(), anyInt(), anyBoolean()))
                .thenReturn(LocalCardListSendResult.failed("offline"));

        assertThatThrownBy(() -> service.sendFull(CODE, false)).isInstanceOf(LocalCardListException.class)
                .hasMessageContaining("offline");
        assertThatThrownBy(() -> service.sendDifferential(CODE, List.of(), false))
                .isInstanceOf(LocalCardListException.class);
    }

    @Test
    void unknownPedestalIsRefused() {
        assertThatThrownBy(() -> service.sendFull("HR-NONE", false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changedHouseCardGoesAsDifferentialUpdateToItsPedestals() {
        HouseCard houseCard = houseCard("H9", null);
        houseCard.setId(9L);
        houseCard.setActive(false);
        when(houseCardService.getHouseCard(9L)).thenReturn(houseCard);
        when(houseCardService.findPedestalsForHouseCard(houseCard)).thenReturn(List.of(pedestal));

        service.updateLocalListWith(9L);

        verify(sender).sendLocalList(eq(pedestal), eq(false),
                eq(List.of(new LocalCardListEntry("H9", null, true, false))), eq(1), eq(true));
    }

    @Test
    void unknownHouseCardUpdatesNothing() {
        service.updateLocalListWith(10L);

        verify(sender, never()).sendLocalList(any(), anyBoolean(), anyList(), anyInt(), anyBoolean());
    }

    private void attribute(boolean includeCards, boolean includeHouseCards) {
        when(localListAttributeService.getLocalListAttribute(pedestal.getGroup()))
                .thenReturn(new LocalListAttribute(true, includeCards, includeHouseCards));
    }

    private void houseCards(HouseCard... houseCards) {
        when(houseCardService.getHouseCardsForPedestal(CODE)).thenReturn(new ArrayList<>(List.of(houseCards)));
    }

    private void cards(String... rfids) {
        List<CampingCard> cards = new ArrayList<>();
        for (String rfid : rfids) {
            cards.add(new CampingCard.Builder(rfid, rfid).build());
        }
        when(campingCardDao.findValidCardsForLocalCardList(any(), any())).thenReturn(cards);
    }

    private static HouseCard houseCard(String rfid, LocalDateTime lastUsed) {
        HouseCard houseCard = new HouseCard(HouseCardType.STAFF, rfid, rfid);
        houseCard.setLastUsed(lastUsed);
        return houseCard;
    }
}
