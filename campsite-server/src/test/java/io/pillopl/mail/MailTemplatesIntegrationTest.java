package io.pillopl.mail;

import io.pillopl.group.attribute.NotificationType;
import io.pillopl.infrastructure.Socket;
import io.pillopl.infrastructure.SocketStatus;
import io.pillopl.test.IntegrationTest;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The mail templates of the application render with the models the services put in.
 */
class MailTemplatesIntegrationTest extends IntegrationTest {

    static final LocalDateTime AT = LocalDateTime.of(2026, 7, 10, 8, 15);

    @Autowired MailService mailService;

    @Test
    void socketErrorMailListsTheErrorsAndThePitch() throws MessagingException {
        Map<String, Object> model = pedestal();
        model.put("socket", socket("HR01*P001*1", "A12"));
        model.put("errorFlags", 5L);
        model.put("errorCodes", List.of("BRK", "TMP"));

        String body = render(NotificationType.PEDESTAL_STATUS.getTemplate(), model);

        assertThat(body).contains("socket HR-BAY-P001_1 of pedestal HR-BAY-P001 (Bay campsite)")
                .contains("HR01*P001*1").contains("A12").contains("FAULTED").contains("BRK, TMP");
    }

    @Test
    void socketErrorMailOfAnUncommissionedSocketWithUnknownFlags() throws MessagingException {
        Map<String, Object> model = pedestal();
        model.put("socket", socket(null, null));
        model.put("errorFlags", 4096L);
        model.put("errorCodes", List.of());

        assertThat(render(NotificationType.PEDESTAL_STATUS.getTemplate(), model))
                .contains("not commissioned").contains("unknown (flags 4096)");
    }

    @Test
    void firmwareAndDiagnosticsMailsNameTheReportedStatus() throws MessagingException {
        Map<String, Object> model = pedestal();
        model.put("status", "INSTALLED");
        model.put("date", AT);
        assertThat(render(NotificationType.FIRMWARE_STATUS.getTemplate(), model))
                .contains("firmware update status INSTALLED").contains("2026");

        model.put("status", "UPLOAD_FAILED");
        assertThat(render(NotificationType.DIAGNOSTICS_STATUS.getTemplate(), model))
                .contains("diagnostics upload status UPLOAD_FAILED").contains("request it again");
    }

    @Test
    void timeoutMailOfAPedestalThatNeverSentAMessage() throws MessagingException {
        Map<String, Object> model = pedestal();
        model.put("timeoutInMinutes", 15);

        assertThat(render("mail/pedestal-timeout.ftl", model)).contains("more than 15 minutes").contains("never");
    }

    @Test
    void missingRecipientMailNamesTheNotificationTypeToConfigure() throws MessagingException {
        Map<String, Object> model = pedestal();
        model.put("notificationType", "FIRMWARE_STATUS");
        model.put("status", "DOWNLOAD_FAILED");

        assertThat(render("mail/notification-missing.ftl", model))
                .contains("notification FIRMWARE_STATUS").contains("Reported status: DOWNLOAD_FAILED");
    }

    @Test
    void backToSupplierAndTechnicalErrorMails() throws MessagingException {
        assertThat(render("mail/pedestal-back-to-supplier.ftl", Map.of("pedestalCode", "HR-BAY-P001")))
                .contains("your pedestal HR-BAY-P001");
        assertThat(render(MailService.TEMPLATE_TECHNICAL_ERROR, Map.of("message", "Group validity check failed")))
                .contains("Group validity check failed");
    }

    private String render(String template, Map<String, Object> model) throws MessagingException {
        return mailService.render(Locale.ENGLISH, template, model);
    }

    private static Map<String, Object> pedestal() {
        Map<String, Object> model = new HashMap<>();
        model.put("pedestalCode", "HR-BAY-P001");
        model.put("campsite", "Bay campsite");
        return model;
    }

    private static Socket socket(String socketCode, String pitchNo) {
        Socket socket = new Socket("HR-BAY-P001_1", null, 1);
        socket.setSocketCode(socketCode);
        socket.setPitchNo(pitchNo);
        socket.setStatus(SocketStatus.FAULTED);
        return socket;
    }
}
