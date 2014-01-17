package io.pillopl.util;

import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.api.exception.ApiException;
import io.pillopl.api.exception.ConflictException;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.card.LocalCardListException;
import io.pillopl.group.AccessDeniedException;
import io.pillopl.user.AffiliateSetupException;
import io.pillopl.value.InvalidValueException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.NoSuchElementException;

/**
 * Turns the exceptions of the domain services into the exceptions of the service interfaces. Order matters: the
 * specific exceptions come before their super classes.
 */
public final class ApiExceptionTranslator {

    private ApiExceptionTranslator() {
        throw new AssertionError();
    }

    /**
     * @return the exception to throw to the web layer; exceptions without a translation are returned as they are
     */
    public static RuntimeException translate(RuntimeException e) {
        if (e instanceof ApiException) {
            return e;
        }
        if (e instanceof AccessDeniedException) {
            return new AccessNotAllowedException(e.getMessage(), e);
        }
        if (e instanceof InvalidValueException) {
            return new InvalidInputException(e.getMessage(), e);
        }
        if (e instanceof AffiliateSetupException || e instanceof DataIntegrityViolationException
                || e instanceof LocalCardListException) {
            return new ConflictException(e.getMessage(), e);
        }
        if (e instanceof NoSuchElementException) {
            return new ObjectNotFoundException(e.getMessage() != null ? e.getMessage() : "Object not found");
        }
        if (e instanceof IllegalStateException) {
            return new ConflictException(e.getMessage(), e);
        }
        if (e instanceof IllegalArgumentException) {
            return new InvalidInputException(e.getMessage(), e);
        }
        return e;
    }
}
