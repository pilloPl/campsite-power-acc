package io.pillopl;

import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.api.exception.ConflictException;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.card.LocalCardListException;
import io.pillopl.group.AccessDeniedException;
import io.pillopl.user.AffiliateSetupException;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.value.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionTranslatorTest {

    @Test
    void domainExceptionsBecomeServiceInterfaceExceptions() {
        assertThat(ApiExceptionTranslator.translate(new AccessDeniedException("no")))
                .isInstanceOf(AccessNotAllowedException.class).hasMessage("no");
        assertThat(ApiExceptionTranslator.translate(new InvalidValueException("PitchNo", "too long")))
                .isInstanceOf(InvalidInputException.class);
        assertThat(ApiExceptionTranslator.translate(new AffiliateSetupException("no affiliate group")))
                .isInstanceOf(ConflictException.class);
        assertThat(ApiExceptionTranslator.translate(new DataIntegrityViolationException("unique")))
                .isInstanceOf(ConflictException.class);
        assertThat(ApiExceptionTranslator.translate(new IllegalStateException("already commissioned")))
                .isInstanceOf(ConflictException.class);
        assertThat(ApiExceptionTranslator.translate(new IllegalArgumentException("bad")))
                .isInstanceOf(InvalidInputException.class);
        assertThat(ApiExceptionTranslator.translate(new NoSuchElementException()))
                .isInstanceOf(ObjectNotFoundException.class);
        assertThat(ApiExceptionTranslator.translate(new LocalCardListException("HR-BAY-P001", "offline")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void serviceInterfaceExceptionsAndUnknownExceptionsStayAsTheyAre() {
        ObjectNotFoundException notFound = ObjectNotFoundException.of("Pedestal", "P1");
        NullPointerException npe = new NullPointerException();

        assertThat(ApiExceptionTranslator.translate(notFound)).isSameAs(notFound);
        assertThat(ApiExceptionTranslator.translate(npe)).isSameAs(npe);
    }
}
