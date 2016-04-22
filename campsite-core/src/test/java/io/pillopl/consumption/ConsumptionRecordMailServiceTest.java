package io.pillopl.consumption;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.mail.MailService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsumptionRecordMailServiceTest {

    final MailService mailService = mock(MailService.class);
    final SystemPropertyService systemPropertyService = mock(SystemPropertyService.class);
    final ConsumptionRecordMailService service = new ConsumptionRecordMailService(mailService,
            systemPropertyService);

    ConsumptionRecord record;

    @BeforeEach
    void setUp() {
        record = new ConsumptionRecord();
        record.setRecordNo("BAY-20260711-000042");
        record.setPedestalCode("HR-BAY-P001");
        record.setPitchNo("A12");
        record.setKwh(new BigDecimal("900.000"));
        record.setPlugIn(LocalDateTime.of(2026, 7, 10, 18, 0));
        record.setPlugOut(LocalDateTime.of(2026, 7, 11, 9, 0));
        record.setNights(1);
        record.markSuspect("Consumption 900 kWh in 1 night(s) is above 300 kWh per night");
        when(systemPropertyService.getDeveloperEmail()).thenReturn("dev@campsite.test");
    }

    @Test
    void suspectRecordGoesToTheReviewersWithItsReason() throws MessagingException {
        when(systemPropertyService.getSuspectRecordEmail()).thenReturn("review@campsite.test");

        service.suspectRecordCreated(record);

        Map<String, Object> model = sentModel("review@campsite.test");
        assertThat(model).containsEntry("reason", "Consumption 900 kWh in 1 night(s) is above 300 kWh per night")
                .containsEntry("kwh", "900").containsEntry("campsite", "N/A").containsEntry("pitchNo", "A12")
                .containsEntry("source", null);
    }

    @Test
    void withoutReviewersTheMailGoesToTheDeveloper() throws MessagingException {
        record.setSourceName("RECORD CLUB");

        service.suspectRecordCreated(record);

        assertThat(sentModel("dev@campsite.test")).containsEntry("source", "RECORD CLUB");
    }

    @Test
    void withoutAnyAddressNothingIsSent() {
        when(systemPropertyService.getDeveloperEmail()).thenReturn(null);

        service.suspectRecordCreated(record);

        verifyNoInteractions(mailService);
    }

    @Test
    void failingMailDoesNotFailTheRecord() throws MessagingException {
        doThrow(new MessagingException("no SMTP")).when(mailService)
                .sendTemplateMail(any(), anyString(), anyString(), anyString(), anyMap());

        service.suspectRecordCreated(record);

        sentModel("dev@campsite.test");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sentModel(String to) throws MessagingException {
        ArgumentCaptor<Map<String, Object>> model = ArgumentCaptor.forClass(Map.class);
        verify(mailService).sendTemplateMail(eq(MailService.DEFAULT_LOCALE), eq(to),
                eq("Consumption record BAY-20260711-000042 waits for a review"),
                eq(ConsumptionRecordMailService.TEMPLATE_SUSPECT), model.capture());
        return model.getValue();
    }
}
