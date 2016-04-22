package io.pillopl.consumption;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.mail.MailService;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Mail to the reviewers of the consumption records ({@code SuspectRecordEmail}, else the developer address): a
 * suspect record waits in the review queue and is neither priced by the network nor exported until someone decides.
 */
@Service
public class ConsumptionRecordMailService implements SuspectRecordNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionRecordMailService.class);

    static final String TEMPLATE_SUSPECT = "mail/consumption-record-suspect.ftl";

    private final MailService mailService;

    private final SystemPropertyService systemPropertyService;

    public ConsumptionRecordMailService(MailService mailService, SystemPropertyService systemPropertyService) {
        this.mailService = mailService;
        this.systemPropertyService = systemPropertyService;
    }

    @Override
    public void suspectRecordCreated(ConsumptionRecord record) {
        String to = systemPropertyService.getSuspectRecordEmail();
        if (to == null || to.isBlank()) {
            to = systemPropertyService.getDeveloperEmail();
        }
        if (to == null || to.isBlank()) {
            LOGGER.warn(String.format("Nobody to tell about suspect consumption record %s", record.getRecordNo()));
            return;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("recordNo", record.getRecordNo());
        model.put("reason", record.getSuspectReason());
        model.put("campsite", record.getCampsiteName() != null ? record.getCampsiteName() : "N/A");
        model.put("pedestalCode", record.getPedestalCode() != null ? record.getPedestalCode() : "N/A");
        model.put("pitchNo", record.getPitchNo() != null ? record.getPitchNo() : "N/A");
        model.put("guestName", record.getGuestName() != null ? record.getGuestName() : "N/A");
        model.put("kwh", record.getKwh() != null ? record.getKwh().stripTrailingZeros().toPlainString() : "unknown");
        model.put("plugIn", record.getPlugIn());
        model.put("plugOut", record.getPlugOut());
        model.put("nights", record.getNights());
        model.put("source", record.isImported() ? record.getSourceName() : null);

        try {
            mailService.sendTemplateMail(MailService.DEFAULT_LOCALE, to, "Consumption record " + record.getRecordNo()
                    + " waits for a review", TEMPLATE_SUSPECT, model);
        } catch (MessagingException e) {
            LOGGER.error(String.format("Suspect record mail of %s to %s failed", record.getRecordNo(), to), e);
        }
    }
}
