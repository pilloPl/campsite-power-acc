package io.pillopl.mail;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.pillopl.audit.AuditService;
import io.pillopl.config.SystemPropertyService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Sends the mails of the pedestal backend: FreeMarker templates (classpath {@code templates/}) rendered to plain text.
 * Without an SMTP server ({@code spring.mail.host} not set) there is no mail sender and the mail is only logged.
 * Every mail is audited.
 */
@Service
public class MailService {

    private static final Logger LOG = LoggerFactory.getLogger(MailService.class);

    public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    static final String TEMPLATE_TECHNICAL_ERROR = "mail/technical-error.ftl";

    private final Configuration freemarker;

    private final ObjectProvider<JavaMailSender> mailSender;

    private final SystemPropertyService systemPropertyService;

    private final AuditService auditService;

    public MailService(Configuration freemarker, ObjectProvider<JavaMailSender> mailSender,
                       SystemPropertyService systemPropertyService, AuditService auditService) {
        this.freemarker = freemarker;
        this.mailSender = mailSender;
        this.systemPropertyService = systemPropertyService;
        this.auditService = auditService;
    }

    /**
     * Renders the template with the model and sends it.
     *
     * @param locale locale of the template lookup, null for the default
     */
    public void sendTemplateMail(Locale locale, String to, String subject, String template, Map<String, Object> model)
            throws MessagingException {
        sendMail(new Mail(to, subject, render(locale, template, model)));
    }

    public void sendMail(Mail mail) throws MessagingException {
        if (mail.to() == null || mail.to().isBlank()) {
            throw new MessagingException("Mail '" + mail.subject() + "' has no recipient");
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            LOG.info(String.format("No SMTP server configured, mail '%s' to %s not sent:%n%s", mail.subject(),
                    mail.to(), mail.body()));
            auditService.auditSendEmail(mail.to(), mail.subject(), mail.body(), null);
            return;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(systemPropertyService.getMailSenderAddress());
            helper.setTo(mail.recipients());
            helper.setSubject(mail.subject());
            helper.setText(mail.body());
            sender.send(message);
            auditService.auditSendEmail(mail.to(), mail.subject(), mail.body(), null);
        } catch (MailException e) {
            auditService.auditSendEmail(mail.to(), mail.subject(), mail.body(), e.getMessage());
            throw new MessagingException("A mail exception was thrown.", e);
        }
    }

    /**
     * Sends an error mail to the developer (SystemProperty DeveloperEmail). Never throws.
     */
    public void sendErrorMail(String subject, String cause) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("message", cause);
        sendErrorMail(subject, TEMPLATE_TECHNICAL_ERROR, parameters);
    }

    public void sendErrorMail(Throwable cause) {
        sendErrorMail("Unhandled exception - " + cause.getMessage(), String.valueOf(cause));
    }

    public void sendErrorMail(String subject, String template, Map<String, Object> parameters) {
        String developerEmail = systemPropertyService.getDeveloperEmail();
        if (developerEmail == null || developerEmail.isBlank()) {
            LOG.error(String.format("No developer email configured, error mail '%s' not sent: %s", subject,
                    parameters));
            return;
        }
        try {
            sendTemplateMail(DEFAULT_LOCALE, developerEmail, subject, template, parameters);
        } catch (Exception ex) {
            LOG.error("Unable to send error mail due to technical problems!", ex);
        }
    }

    String render(Locale locale, String template, Map<String, Object> model) throws MessagingException {
        try {
            Template t = freemarker.getTemplate(template, locale != null ? locale : DEFAULT_LOCALE);
            return FreeMarkerTemplateUtils.processTemplateIntoString(t, model);
        } catch (IOException | TemplateException e) {
            throw new MessagingException("Mail template " + template + " could not be rendered", e);
        }
    }
}
