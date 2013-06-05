package io.pillopl;

import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.group.AccessMode;
import io.pillopl.group.GroupType;
import io.pillopl.util.ApiValues;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiValuesTest {

    @Test
    void enumNamesAreReadIgnoringCaseBlanksAndDashes() {
        assertThat(ApiValues.parseEnum(AccessMode.class, "house cards only", "access mode"))
                .isEqualTo(AccessMode.HOUSE_CARDS_ONLY);
        assertThat(ApiValues.parseEnum(AccessMode.class, "network-access", "access mode"))
                .isEqualTo(AccessMode.NETWORK_ACCESS);
        assertThat(ApiValues.parseEnum(GroupType.class, " site ", "group type")).isEqualTo(GroupType.SITE);
    }

    @Test
    void missingValueIsNullOnlyWhereOptional() {
        assertThat(ApiValues.parseEnumOrNull(GroupType.class, " ", "group type")).isNull();
        assertThatThrownBy(() -> ApiValues.parseEnum(GroupType.class, null, "group type"))
                .isInstanceOf(InvalidInputException.class).hasMessageContaining("missing");
    }

    @Test
    void unknownValueIsRejectedWithItsName() {
        assertThatThrownBy(() -> ApiValues.parseEnum(GroupType.class, "CAMPSITE", "group type"))
                .isInstanceOf(InvalidInputException.class).hasMessageContaining("group type");
    }
}
