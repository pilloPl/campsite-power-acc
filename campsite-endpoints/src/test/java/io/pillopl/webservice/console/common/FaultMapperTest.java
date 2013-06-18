package io.pillopl.webservice.console.common;

import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.api.exception.ConflictException;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.value.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class FaultMapperTest {

    @Test
    void serviceInterfaceExceptionsGetTheirErrorCodes() {
        assertThat(FaultMapper.toErrorCode(ObjectNotFoundException.of("Pedestal", "P1")))
                .isEqualTo(WsErrorCode.NOT_EXISTS);
        assertThat(FaultMapper.toErrorCode(new AccessNotAllowedException("no"))).isEqualTo(WsErrorCode.NOT_AUTHORIZED);
        assertThat(FaultMapper.toErrorCode(new InvalidInputException("bad"))).isEqualTo(WsErrorCode.WRONG_REQUEST);
        assertThat(FaultMapper.toErrorCode(new ConflictException("busy"))).isEqualTo(WsErrorCode.CONFLICT);
    }

    @Test
    void invalidRequestValuesAreWrongRequests() {
        assertThat(FaultMapper.toErrorCode(new InvalidValueException("Username", "is missing")))
                .isEqualTo(WsErrorCode.WRONG_REQUEST);
        assertThat(FaultMapper.toErrorCode(new IllegalArgumentException("Rows per page must be between")))
                .isEqualTo(WsErrorCode.WRONG_REQUEST);
    }

    @Test
    void databaseErrorsAreTechnicalExceptUniqueViolations() {
        assertThat(FaultMapper.toErrorCode(new DataIntegrityViolationException("unique")))
                .isEqualTo(WsErrorCode.ALREADY_EXISTS);
        assertThat(FaultMapper.toErrorCode(new QueryTimeoutException("slow"))).isEqualTo(WsErrorCode.TECHNICAL_ERROR);
        assertThat(FaultMapper.toErrorCode(new NullPointerException())).isEqualTo(WsErrorCode.TECHNICAL_ERROR);
        assertThat(FaultMapper.toErrorCode(new UnsupportedOperationException()))
                .isEqualTo(WsErrorCode.METHOD_NOT_AVAILABLE);
    }

    @Test
    void technicalErrorsDoNotShowTheirMessage() {
        assertThat(FaultMapper.toMessage(new QueryTimeoutException("select * from secret"),
                WsErrorCode.TECHNICAL_ERROR)).isEqualTo("Data access error");
        assertThat(FaultMapper.toMessage(new NullPointerException("x"), WsErrorCode.TECHNICAL_ERROR))
                .isEqualTo("Technical error");
        assertThat(FaultMapper.toMessage(new ConflictException("Pedestal already commissioned"),
                WsErrorCode.CONFLICT)).isEqualTo("Pedestal already commissioned");
    }

    @Test
    void errorCodesCarryTheirHttpStatus() {
        assertThat(WsErrorCode.NOT_EXISTS.getHttpStatus()).isEqualTo(404);
        assertThat(WsErrorCode.CONFLICT.getHttpStatus()).isEqualTo(409);
        assertThat(WsErrorCode.NOT_AUTHORIZED.getHttpStatus()).isEqualTo(403);
    }
}
