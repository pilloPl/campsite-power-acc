package io.pillopl.monitoring;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.group.attribute.Notification;
import io.pillopl.group.attribute.NotificationService;
import io.pillopl.group.attribute.NotificationType;
import io.pillopl.infrastructure.Location;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalLinkVersion;
import io.pillopl.infrastructure.PedestalType;
import io.pillopl.infrastructure.Socket;
import io.pillopl.mail.MailService;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.SocketStatusLogDao;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PedestalMailServiceTest {

    static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 10, 8, 0);

    final MailService mailService = mock(MailService.class);
    final NotificationService notificationService = mock(NotificationService.class);
    final SocketStatusLogDao socketStatusLogDao = mock(SocketStatusLogDao.class);
    final SystemPropertyService systemPropertyService = mock(SystemPropertyService.class);

    final PedestalDao pedestalDao = mock(PedestalDao.class);
    final SocketDao socketDao = mock(SocketDao.class);

    final PedestalMailService pedestalMailService = new PedestalMailService(mailService, notificationService,
            socketStatusLogDao, systemPropertyService, pedestalDao, socketDao);

    Pedestal pedestal;
    Socket socket;

    @BeforeEach
    void setUp() {
        Location location = new Location("Rovinj", "52210", "Obala", "12");
        location.setName("Bay campsite");
        pedestal = new Pedestal("HR-BAY-P001", new PedestalType("ACME PEDESTALS", "P4", PedestalLinkVersion.V1),
                location);
        socket = new Socket("HR-BAY-P001_1", null, 1);
        pedestal.addSocket(socket);
        when(systemPropertyService.getDeveloperEmail()).thenReturn("dev@campsite.test");
        when(socketStatusLogDao.findErrorAbbreviations("ACME PEDESTALS", 5L)).thenReturn(List.of("BRK", "TMP"));
    }

    @Test
    void socketErrorGoesToTheNearestServiceRecipientWithTheErrorsInTheSubject() throws MessagingException {
        when(notificationService.fetchNotification(pedestal, NotificationType.PEDESTAL_STATUS))
                .thenReturn(new Notification("Bay service", "service@bay.test", NotificationType.PEDESTAL_STATUS));

        pedestalMailService.sendErrorMail(socket, 5L);

        Map<String, Object> model = sentModel("service@bay.test", "Socket HR-BAY-P001_1 of pedestal HR-BAY-P001: BRK, TMP",
                "mail/pedestal-status.ftl");
        assertThat(model).containsEntry("errorCodes", List.of("BRK", "TMP")).containsEntry("socket", socket)
                .containsEntry("campsite", "Bay campsite");
    }

    @Test
    void socketErrorWithoutServiceRecipientGoesToTheDeveloperWithAConfigurationWarning() throws MessagingException {
        pedestalMailService.sendErrorMail(socket, 5L);

        sentModel("dev@campsite.test", "Socket problem and no service recipient configured!",
                PedestalMailService.TEMPLATE_NOTIFICATION_MISSING);
    }

    @Test
    void failingRecipientLookupStillSendsTheErrorToTheDeveloper() throws MessagingException {
        when(notificationService.fetchNotification(pedestal, NotificationType.PEDESTAL_STATUS))
                .thenThrow(new IllegalStateException("tree broken"));

        pedestalMailService.sendErrorMail(socket, 5L);

        sentModel("dev@campsite.test", "Socket problem and no service recipient configured!",
                PedestalMailService.TEMPLATE_NOTIFICATION_MISSING);
    }

    @Test
    void timeoutMailNamesTheLastMessageAndTheTimeout() throws MessagingException {
        when(systemPropertyService.getPedestalHeartbeatTimeoutInMinutes()).thenReturn(15);
        when(notificationService.fetchNotification(pedestal, NotificationType.PEDESTAL_STATUS))
                .thenReturn(new Notification("Bay service", "service@bay.test", NotificationType.PEDESTAL_STATUS));

        pedestalMailService.sendTimeoutMail(pedestal, NOW);

        Map<String, Object> model = sentModel("service@bay.test", "Pedestal HR-BAY-P001 in timeout",
                PedestalMailService.TEMPLATE_TIMEOUT);
        assertThat(model).containsEntry("lastIncoming", NOW).containsEntry("timeoutInMinutes", 15);
    }

    @Test
    void firmwareStatusGoesToTheFirmwareRecipient() throws MessagingException {
        when(notificationService.fetchNotification(pedestal, NotificationType.FIRMWARE_STATUS))
                .thenReturn(new Notification("Firmware", "firmware@bay.test", NotificationType.FIRMWARE_STATUS));

        pedestalMailService.sendStatusMail(pedestal, NotificationType.FIRMWARE_STATUS, "INSTALLED", NOW);

        Map<String, Object> model = sentModel("firmware@bay.test", "Firmware update of pedestal HR-BAY-P001: INSTALLED",
                "mail/pedestal-firmware-status.ftl");
        assertThat(model).containsEntry("status", "INSTALLED").containsEntry("date", NOW);
    }

    @Test
    void diagnosticsStatusWithoutRecipientGoesToTheDeveloper() throws MessagingException {
        pedestalMailService.sendStatusMail(pedestal, NotificationType.DIAGNOSTICS_STATUS, "UPLOAD_FAILED", NOW);

        Map<String, Object> model = sentModel("dev@campsite.test",
                "Diagnostics UPLOAD_FAILED and no service recipient configured!",
                PedestalMailService.TEMPLATE_NOTIFICATION_MISSING);
        assertThat(model).containsEntry("notificationType", "DIAGNOSTICS_STATUS");
    }

    @Test
    void ownerIsToldThatThePedestalWentBackToTheSupplierInItsLocale() throws MessagingException {
        pedestalMailService.pedestalBackToSupplier(Locale.ITALIAN, "HR-BAY-P001", "owner@bay.test");

        verify(mailService).sendTemplateMail(eq(Locale.ITALIAN), eq("owner@bay.test"),
                eq("Your pedestal HR-BAY-P001 went back to the supplier"),
                eq(PedestalMailService.TEMPLATE_BACK_TO_SUPPLIER), eq(Map.of("pedestalCode", "HR-BAY-P001")));
    }

    @Test
    void failedBackToSupplierMailDoesNotStopTheDecommissioning() throws MessagingException {
        doThrow(new MessagingException("no SMTP")).when(mailService)
                .sendTemplateMail(any(), anyString(), anyString(), anyString(), anyMap());

        pedestalMailService.pedestalBackToSupplier(Locale.ENGLISH, "HR-BAY-P001", "owner@bay.test");
    }

    @Test
    void powerSectionAlertGoesToTheServiceRecipientOfThePedestal() throws MessagingException {
        when(pedestalDao.findByCode("HR-BAY-P001")).thenReturn(pedestal);
        when(notificationService.fetchNotification(pedestal, NotificationType.PEDESTAL_STATUS))
                .thenReturn(new Notification("Bay service", "service@bay.test", NotificationType.PEDESTAL_STATUS));

        pedestalMailService.sendPowerSectionMisconfiguredMail("HR-BAY-P001", 1001L, 2, "No budget algorithm");

        Map<String, Object> model = sentModel("service@bay.test",
                "Power section 1001 refuses supplies: No budget algorithm",
                PedestalMailService.TEMPLATE_POWER_SECTION_MISCONFIGURED);
        assertThat(model).containsEntry("powerSectionId", "1001").containsEntry("socketNo", "2")
                .containsEntry("problem", "No budget algorithm").containsEntry("campsite", "Bay campsite");
    }

    @Test
    void powerSectionAlertOfAnUnknownPedestalGoesToTheDeveloper() throws MessagingException {
        pedestalMailService.sendPowerSectionMisconfiguredMail("HR-BAY-P001", 1001L, null, "Unknown algorithm");

        Map<String, Object> model = sentModel("dev@campsite.test",
                "Power section 1001 refuses supplies: Unknown algorithm",
                PedestalMailService.TEMPLATE_POWER_SECTION_MISCONFIGURED);
        assertThat(model).containsEntry("socketNo", "N/A").containsEntry("campsite", "N/A");
    }

    @Test
    void qrPaymentErrorGoesToTheServiceRecipientOfTheSocketsPedestal() throws MessagingException {
        when(socketDao.findById(77L)).thenReturn(java.util.Optional.of(socket));
        when(notificationService.fetchNotification(pedestal, NotificationType.PEDESTAL_STATUS))
                .thenReturn(new Notification("Bay service", "service@bay.test", NotificationType.PEDESTAL_STATUS));

        pedestalMailService.sendQrPayErrorMail(77L, 5005L, "CALCULATION_FAILED", "no tariff");

        Map<String, Object> model = sentModel("service@bay.test", "QR payment 5005 failed: CALCULATION_FAILED",
                PedestalMailService.TEMPLATE_QRPAY_ERROR);
        assertThat(model).containsEntry("processId", "5005").containsEntry("message", "no tariff");
    }

    @Test
    void qrPaymentErrorWithoutSocketGoesToTheDeveloper() throws MessagingException {
        pedestalMailService.sendQrPayErrorMail(null, 5006L, "UNSUPPORTED_OPERATION", null);

        verify(mailService).sendTemplateMail(eq(MailService.DEFAULT_LOCALE), eq("dev@campsite.test"),
                eq("QR payment 5006 failed: UNSUPPORTED_OPERATION"), eq(PedestalMailService.TEMPLATE_QRPAY_ERROR),
                eq(Map.of("pedestalCode", "N/A", "campsite", "N/A", "processId", "5006",
                        "errorCode", "UNSUPPORTED_OPERATION", "message", "N/A")));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sentModel(String to, String subject, String template) throws MessagingException {
        ArgumentCaptor<Map<String, Object>> model = ArgumentCaptor.forClass(Map.class);
        verify(mailService).sendTemplateMail(eq(MailService.DEFAULT_LOCALE), eq(to), eq(subject), eq(template),
                model.capture());
        assertThat(model.getValue()).containsEntry("pedestalCode", "HR-BAY-P001");
        return model.getValue();
    }
}
