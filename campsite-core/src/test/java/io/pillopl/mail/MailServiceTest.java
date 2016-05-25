package io.pillopl.mail;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import io.pillopl.audit.AuditService;
import io.pillopl.config.SystemPropertyService;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailServiceTest {

    private final SystemPropertyService systemPropertyService = mock(SystemPropertyService.class);

    private final AuditService auditService = mock(AuditService.class);

    private final JavaMailSender javaMailSender = mock(JavaMailSender.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> senderProvider = mock(ObjectProvider.class);

    private MailService mailService;

    @BeforeEach
    void setUp() {
        StringTemplateLoader loader = new StringTemplateLoader();
        loader.putTemplate("mail/hello.ftl", "Hello ${name}, socket ${socket} is ${status}.");
        loader.putTemplate(MailService.TEMPLATE_TECHNICAL_ERROR, "Error: ${message}");
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);
        configuration.setTemplateLoader(loader);
        mailService = new MailService(configuration, senderProvider, systemPropertyService, auditService);
        when(systemPropertyService.getMailSenderAddress()).thenReturn("noreply@campsite.test");
        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void withoutSmtpServerTheMailIsOnlyLoggedAndAudited() throws MessagingException {
        when(senderProvider.getIfAvailable()).thenReturn(null);

        mailService.sendTemplateMail(null, "service@campsite.test", "Socket fault", "mail/hello.ftl",
                Map.of("name", "Service", "socket", "HR01*P1*1", "status", "FAULTED"));

        verify(auditService).auditSendEmail("service@campsite.test", "Socket fault",
                "Hello Service, socket HR01*P1*1 is FAULTED.", null);
    }

    @Test
    void withSmtpServerTheMailGoesFromTheConfiguredSenderToAllRecipients() throws Exception {
        when(senderProvider.getIfAvailable()).thenReturn(javaMailSender);

        mailService.sendMail(new Mail("a@campsite.test, b@campsite.test", "Subject", "Body"));

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(sent.capture());
        assertThat(sent.getValue().getFrom()[0].toString()).isEqualTo("noreply@campsite.test");
        assertThat(Arrays.stream(sent.getValue().getAllRecipients()).map(Address::toString))
                .containsExactly("a@campsite.test", "b@campsite.test");
        assertThat(sent.getValue().getSubject()).isEqualTo("Subject");
        verify(auditService).auditSendEmail("a@campsite.test, b@campsite.test", "Subject", "Body", null);
    }

    @Test
    void failingSmtpServerIsAuditedAsErrorAndReportedToTheCaller() {
        when(senderProvider.getIfAvailable()).thenReturn(javaMailSender);
        doThrow(new MailSendException("connection refused")).when(javaMailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> mailService.sendMail(new Mail("a@campsite.test", "Subject", "Body")))
                .isInstanceOf(MessagingException.class);
        verify(auditService).auditSendEmail("a@campsite.test", "Subject", "Body", "connection refused");
    }

    @Test
    void mailWithoutRecipientIsRefused() {
        assertThatThrownBy(() -> mailService.sendMail(new Mail(" ", "Subject", "Body")))
                .isInstanceOf(MessagingException.class);
        verify(auditService, never()).auditSendEmail(any(), any(), any(), any());
    }

    @Test
    void missingTemplateIsReportedToTheCaller() {
        assertThatThrownBy(() -> mailService.sendTemplateMail(null, "a@campsite.test", "Subject",
                "mail/missing.ftl", Map.of()))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("mail/missing.ftl");
    }

    @Test
    void errorMailGoesToTheDeveloper() {
        when(systemPropertyService.getDeveloperEmail()).thenReturn("dev@campsite.test");

        mailService.sendErrorMail("Distribution stopped", "after 120 minutes");

        verify(auditService).auditSendEmail("dev@campsite.test", "Distribution stopped", "Error: after 120 minutes",
                null);
    }

    @Test
    void errorMailWithoutDeveloperAddressIsOnlyLogged() {
        when(systemPropertyService.getDeveloperEmail()).thenReturn(null);

        mailService.sendErrorMail("Distribution stopped", "after 120 minutes");

        verify(auditService, never()).auditSendEmail(anyString(), anyString(), anyString(), isNull());
    }

    @Test
    void errorMailNeverThrowsEvenWhenSmtpFails() {
        when(systemPropertyService.getDeveloperEmail()).thenReturn("dev@campsite.test");
        when(senderProvider.getIfAvailable()).thenReturn(javaMailSender);
        doThrow(new MailSendException("down")).when(javaMailSender).send(any(MimeMessage.class));

        mailService.sendErrorMail(new IllegalStateException("boom"));

        verify(auditService).auditSendEmail(eq("dev@campsite.test"), eq("Backend failure: boom"), anyString(),
                eq("down"));
    }
}
