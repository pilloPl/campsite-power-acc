package io.pillopl.jobs;

import io.pillopl.audit.AuditService;
import io.pillopl.card.LocalCardListDistributionService;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.group.GroupService;
import io.pillopl.mail.MailService;
import io.pillopl.monitoring.PedestalStateService;
import io.pillopl.user.UserinfoSummaryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonitoringAndMaintenanceJobsTest {

    final PedestalStateService pedestalStateService = mock(PedestalStateService.class);
    final SystemPropertyService systemPropertyService = mock(SystemPropertyService.class);
    final GroupService groupService = mock(GroupService.class);
    final MailService mailService = mock(MailService.class);

    @Test
    void heartbeatTimeoutJobTimesOutWithTheConfiguredTimeout() {
        when(systemPropertyService.getPedestalHeartbeatTimeoutInMinutes()).thenReturn(15);

        new PedestalHeartbeatTimeoutJob(pedestalStateService, systemPropertyService).executeInternal(null);

        verify(pedestalStateService).timeoutPedestals(15);
    }

    @Test
    void unknownStateJobSendsEventsWithTheConfiguredTimeout() {
        when(systemPropertyService.getUnknownStateTimeoutInMinutes()).thenReturn(180);

        new UnknownStateEventTriggerJob(pedestalStateService, systemPropertyService).executeInternal(null);

        verify(pedestalStateService).sendUnknownEvents(180);
    }

    @Test
    void groupValidityErrorsAreMailedToTheDeveloper() {
        when(groupService.collectValidityErrors()).thenReturn(List.of("No group attached to Pedestal X",
                "Guest Y is attached to the wrong group SITE"));

        new GroupValidityControlJob(groupService, mailService).executeInternal(null);

        verify(mailService).sendErrorMail("Group validity check found 2 error(s)",
                "No group attached to Pedestal X" + System.lineSeparator()
                        + "Guest Y is attached to the wrong group SITE");
    }

    @Test
    void validGroupTreesSendNoMail() {
        when(groupService.collectValidityErrors()).thenReturn(List.of());

        new GroupValidityControlJob(groupService, mailService).executeInternal(null);

        verify(mailService, never()).sendErrorMail(anyString(), anyString());
    }

    @Test
    void refreshAndCleanupJobsOnlyCallTheirService() {
        UserinfoSummaryService userinfoSummaryService = mock(UserinfoSummaryService.class);
        AuditService auditService = mock(AuditService.class);
        LocalCardListDistributionService distributionService = mock(LocalCardListDistributionService.class);

        new UserinfoSummaryRefreshJob(userinfoSummaryService).executeInternal(null);
        new SocketUtilizationRefreshJob(pedestalStateService).executeInternal(null);
        new AuditCleanupJob(auditService).executeInternal(null);
        new LocalCardListDistributionJob(distributionService).executeInternal(null);

        verify(userinfoSummaryService).refresh();
        verify(pedestalStateService).refreshSocketUtilization();
        verify(auditService).cleanAuditTable();
        verify(distributionService).distribute();
    }
}
