package io.pillopl.issuer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import static io.pillopl.issuer.PartnerSyncState.ADD;
import static io.pillopl.issuer.PartnerSyncState.DELETE;
import static io.pillopl.issuer.PartnerSyncState.MODIFY;
import static io.pillopl.issuer.PartnerSyncState.NEVER;
import static io.pillopl.issuer.PartnerSyncState.NOT;
import static io.pillopl.issuer.PartnerSyncState.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

class PartnerSyncStateTest {

    @ParameterizedTest(name = "wanted {0} for a card never exported -> {1}")
    @CsvSource({"ADD, ADD", "DELETE, NOT", "MODIFY, ADD", "UNKNOWN, UNKNOWN", "NOT, NOT", "NEVER, NEVER"})
    void cardNeverExportedIsAddedOrLeftOut(PartnerSyncState wanted, PartnerSyncState exported) {
        assertThat(wanted.getByTargetPartnerSyncState(null)).isEqualTo(exported);
    }

    @ParameterizedTest(name = "ADD when the partners got {0} -> {1}")
    @CsvSource({"ADD, NOT", "DELETE, ADD", "MODIFY, MODIFY", "UNKNOWN, UNKNOWN", "NOT, UNKNOWN", "NEVER, NEVER"})
    void addOfACardThePartnersHaveIsNothingToDo(PartnerSyncState target, PartnerSyncState exported) {
        assertThat(ADD.getByTargetPartnerSyncState(target)).isEqualTo(exported);
    }

    @ParameterizedTest(name = "DELETE when the partners got {0} -> {1}")
    @CsvSource({"ADD, DELETE", "DELETE, NOT", "MODIFY, DELETE", "UNKNOWN, UNKNOWN", "NOT, UNKNOWN", "NEVER, NEVER"})
    void deleteIsExportedOnlyForACardThePartnersHave(PartnerSyncState target, PartnerSyncState exported) {
        assertThat(DELETE.getByTargetPartnerSyncState(target)).isEqualTo(exported);
    }

    @ParameterizedTest(name = "MODIFY when the partners got {0} -> {1}")
    @CsvSource({"ADD, MODIFY", "DELETE, ADD", "MODIFY, MODIFY", "UNKNOWN, UNKNOWN", "NOT, UNKNOWN", "NEVER, NEVER"})
    void modifyOfADeletedCardIsAnAdd(PartnerSyncState target, PartnerSyncState exported) {
        assertThat(MODIFY.getByTargetPartnerSyncState(target)).isEqualTo(exported);
    }

    @ParameterizedTest(name = "UNKNOWN when the partners got {0}")
    @EnumSource(value = PartnerSyncState.class, names = "NEVER", mode = EnumSource.Mode.EXCLUDE)
    void unknownStaysUnknownUnlessTheCardIsNeverExchanged(PartnerSyncState target) {
        assertThat(UNKNOWN.getByTargetPartnerSyncState(target)).isEqualTo(UNKNOWN);
        assertThat(UNKNOWN.getByTargetPartnerSyncState(NEVER)).isEqualTo(NEVER);
    }

    @ParameterizedTest(name = "NOT when the partners got {0} -> {1}")
    @CsvSource({"ADD, NOT", "DELETE, NOT", "MODIFY, NOT", "UNKNOWN, UNKNOWN", "NOT, NOT", "NEVER, NEVER"})
    void notKeepsTheCardOutOfTheExport(PartnerSyncState target, PartnerSyncState exported) {
        assertThat(NOT.getByTargetPartnerSyncState(target)).isEqualTo(exported);
    }

    @ParameterizedTest(name = "NEVER when the partners got {0}")
    @EnumSource(PartnerSyncState.class)
    void neverIsForGood(PartnerSyncState target) {
        assertThat(NEVER.getByTargetPartnerSyncState(target)).isEqualTo(NEVER);
        assertThat(NEVER.getByTargetPartnerSyncState(null)).isEqualTo(NEVER);
    }

    @Test
    void onlyAddModifyAndDeleteAreSentToThePartners() {
        assertThat(ADD.isExported()).isTrue();
        assertThat(MODIFY.isExported()).isTrue();
        assertThat(DELETE.isExported()).isTrue();
        assertThat(UNKNOWN.isExported()).isFalse();
        assertThat(NOT.isExported()).isFalse();
        assertThat(NEVER.isExported()).isFalse();
    }
}
