package io.pillopl.value;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserValuesTest {

    @Test
    void usernameIsTrimmedAndHasNoBlanks() {
        assertThat(Username.of(" reception.bay ").value()).isEqualTo("reception.bay");
        assertThatThrownBy(() -> Username.of("reception bay")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> Username.of("")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void emailNeedsOneAtAndADomainWithADot() {
        EmailAddress email = EmailAddress.of(" owner@bay-camp.hr ");

        assertThat(email.value()).isEqualTo("owner@bay-camp.hr");
        assertThat(email.localPart()).isEqualTo("owner");
        assertThatThrownBy(() -> EmailAddress.of("owner")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> EmailAddress.of("@bay.hr")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> EmailAddress.of("a@b@bay.hr")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> EmailAddress.of("owner@localhost")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> EmailAddress.of("owner@bay.")).isInstanceOf(InvalidValueException.class);
        assertThat(EmailAddress.ofNullable(" ")).isNull();
    }

    @Test
    void installerTokenIsAUuidStoredLowerCase() {
        assertThat(InstallerToken.of("3F2504E0-4F89-11D3-9A0C-0305E82C3301").value())
                .isEqualTo("3f2504e0-4f89-11d3-9a0c-0305e82c3301");
        assertThatThrownBy(() -> InstallerToken.of("not-a-token")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void groupNameMustNotContainThePathSeparator() {
        assertThat(GroupName.of(" BAY CAMPSITE ").value()).isEqualTo("BAY CAMPSITE");
        assertThatThrownBy(() -> GroupName.of("HR SITES | BAY")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> GroupName.of(" ")).isInstanceOf(InvalidValueException.class);
    }
}
