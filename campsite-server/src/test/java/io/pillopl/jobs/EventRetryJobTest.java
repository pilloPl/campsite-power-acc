package io.pillopl.jobs;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.event.EventRetryService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventRetryJobTest {

    @Test
    void jobOnlyAsksToSendTheKeptEventsAgainWithTheConfiguredAttempts() {
        EventRetryService eventRetryService = mock(EventRetryService.class);
        SystemPropertyService systemPropertyService = mock(SystemPropertyService.class);
        when(systemPropertyService.getEventRetryMaxAttempts()).thenReturn(7);

        new EventRetryJob(eventRetryService, systemPropertyService).executeInternal(null);

        verify(eventRetryService).retryOpenEvents(7);
    }
}
