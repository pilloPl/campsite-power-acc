package io.pillopl.jobs;

import io.pillopl.group.GroupService;
import io.pillopl.mail.MailService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.List;

/**
 * Nightly control of the group trees; the errors found go to the developer by mail.
 */
@DisallowConcurrentExecution
public class GroupValidityControlJob extends QuartzJobBean {

    private final GroupService groupService;

    private final MailService mailService;

    public GroupValidityControlJob(GroupService groupService, MailService mailService) {
        this.groupService = groupService;
        this.mailService = mailService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        List<String> errors = groupService.collectValidityErrors();
        if (!errors.isEmpty()) {
            mailService.sendErrorMail("Group validity check found " + errors.size() + " error(s)",
                    String.join(System.lineSeparator(), errors));
        }
    }
}
