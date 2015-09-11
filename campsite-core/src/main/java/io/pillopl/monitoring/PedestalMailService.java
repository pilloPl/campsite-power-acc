package io.pillopl.monitoring;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.group.attribute.Notification;
import io.pillopl.group.attribute.NotificationService;
import io.pillopl.group.attribute.NotificationType;
import io.pillopl.infrastructure.DecommissioningNotifier;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.mail.MailService;
import io.pillopl.persistence.dao.SocketStatusLogDao;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Service mails of the pedestals: socket errors, link timeouts, firmware and diagnostics status, and the mail to the
 * affiliate owner when a pedestal goes back to the supplier. The recipient is the {@link Notification} of the type
 * nearest above the pedestal; without one the mail goes to the developer with a configuration warning.
 */
@Service
public class PedestalMailService implements DecommissioningNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(PedestalMailService.class);

    static final String TEMPLATE_NOTIFICATION_MISSING = "mail/notification-missing.ftl";

    static final String TEMPLATE_TIMEOUT = "mail/pedestal-timeout.ftl";

    static final String TEMPLATE_BACK_TO_SUPPLIER = "mail/pedestal-back-to-supplier.ftl";

    private final MailService mailService;

    private final NotificationService notificationService;

    private final SocketStatusLogDao socketStatusLogDao;

    private final SystemPropertyService systemPropertyService;

    public PedestalMailService(MailService mailService, NotificationService notificationService,
                               SocketStatusLogDao socketStatusLogDao, SystemPropertyService systemPropertyService) {
        this.mailService = mailService;
        this.notificationService = notificationService;
        this.socketStatusLogDao = socketStatusLogDao;
        this.systemPropertyService = systemPropertyService;
    }

    /**
     * Error mail of a socket: the error flags read with the masks of the manufacturer.
     */
    public void sendErrorMail(Socket socket, long errorFlags) throws MessagingException {
        Pedestal pedestal = socket.getPedestal();
        LOGGER.error(String.format("Sending out error email for socket '%s' ...", socket.getName()));

        // variables
        List<String> errorCodes = socketStatusLogDao.findErrorAbbreviations(
                pedestal.getPedestalType().getManufacturer(), errorFlags);

        // fetch notification settings
        Notification notification = null;
        try {
            notification = notificationService.fetchNotification(pedestal, NotificationType.PEDESTAL_STATUS);
        } catch (Exception e) {
            LOGGER.error(String.format("Notification couldn't be fetched for socket '%s' ...", socket.getName()), e);
        }

        // prepare template
        String errorNames = String.join(", ", errorCodes);
        String subject;
        String template;
        if (notification != null) {
            subject = String.format("Socket %s of pedestal %s: %s", socket.getName(), pedestal.getCode(), errorNames);
            template = notification.getNotificationType().getTemplate();
        } else {
            subject = "Socket problem and no service recipient configured!";
            template = TEMPLATE_NOTIFICATION_MISSING;
        }

        Map<String, Object> model = model(pedestal);
        model.put("socket", socket);
        model.put("errorFlags", errorFlags);
        model.put("errorCodes", errorCodes);
        model.put("notificationType", NotificationType.PEDESTAL_STATUS.name());

        // send mail
        mailService.sendTemplateMail(MailService.DEFAULT_LOCALE, recipient(notification), subject, template, model);
    }

    /**
     * The link of the pedestal timed out (no message within the heartbeat timeout).
     */
    public void sendTimeoutMail(Pedestal pedestal, LocalDateTime lastIncoming) throws MessagingException {
        LOGGER.error(String.format("Sending out timeout email for pedestal '%s'", pedestal.getCode()));
        Notification notification = notificationService.fetchNotification(pedestal, NotificationType.PEDESTAL_STATUS);

        Map<String, Object> model = model(pedestal);
        model.put("lastIncoming", lastIncoming);
        model.put("timeoutInMinutes", systemPropertyService.getPedestalHeartbeatTimeoutInMinutes());

        mailService.sendTemplateMail(MailService.DEFAULT_LOCALE, recipient(notification),
                "Pedestal " + pedestal.getCode() + " in timeout", TEMPLATE_TIMEOUT, model);
    }

    /**
     * Firmware update or diagnostics upload status reported by the pedestal.
     */
    public void sendStatusMail(Pedestal pedestal, NotificationType notificationType, String status,
                               LocalDateTime at) throws MessagingException {
        // fetch notification settings
        Notification notification;
        try {
            notification = notificationService.fetchNotification(pedestal, notificationType);
        } catch (Exception e) {
            notification = null;
        }

        String topic = notificationType == NotificationType.FIRMWARE_STATUS ? "Firmware update" : "Diagnostics";
        String subject;
        String template;
        if (notification != null) {
            subject = String.format("%s of pedestal %s: %s", topic, pedestal.getCode(), status);
            template = notification.getNotificationType().getTemplate();
        } else {
            subject = String.format("%s %s and no service recipient configured!", topic, status);
            template = TEMPLATE_NOTIFICATION_MISSING;
        }

        Map<String, Object> model = model(pedestal);
        model.put("status", status);
        model.put("date", at);
        model.put("notificationType", notificationType.name());

        mailService.sendTemplateMail(MailService.DEFAULT_LOCALE, recipient(notification), subject, template, model);
    }

    /**
     * Mail to the affiliate owner: the pedestal went back to the supplier. Failures are logged (and audited by the
     * mail service), the decommissioning goes on.
     */
    @Override
    public void pedestalBackToSupplier(Locale locale, String pedestalCode, String email) {
        Map<String, Object> model = new HashMap<>();
        model.put("pedestalCode", pedestalCode);
        try {
            mailService.sendTemplateMail(locale, email, "Your pedestal " + pedestalCode + " went back to the supplier",
                    TEMPLATE_BACK_TO_SUPPLIER, model);
        } catch (MessagingException e) {
            LOGGER.error(String.format("Back to supplier mail for pedestal %s to %s failed", pedestalCode, email), e);
        }
    }

    private String recipient(Notification notification) {
        if (notification != null && notification.getEmail() != null && !notification.getEmail().isBlank()) {
            return notification.getEmail();
        }
        // Fallback
        return systemPropertyService.getDeveloperEmail();
    }

    private static Map<String, Object> model(Pedestal pedestal) {
        Map<String, Object> model = new HashMap<>();
        model.put("pedestalCode", pedestal.getCode());
        model.put("campsite", pedestal.getLocation() != null && pedestal.getLocation().getName() != null
                ? pedestal.getLocation().getName() : "N/A");
        return model;
    }
}
