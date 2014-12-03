package io.pillopl.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.pillopl.group.attribute.CommunicationLogAttributeService;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.monitoring.PedestalCommunicationLog;
import io.pillopl.monitoring.PedestalCommunicationLog.LogType;
import io.pillopl.persistence.dao.PedestalCommunicationLogDao;
import io.pillopl.persistence.dao.PedestalDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Writes the communication log of the pedestals (PEDESTAL_COMM_LOG) from their events, for pedestals whose SITE
 * subtree has the communication log switched on: boots ({@link Boot}) and finished supplies, the source of the
 * consumption records ({@link ConsumptionRecord}). The event attributes are logged as JSON.
 */
public abstract class PedestalCommunicationLogListener extends EventListener {

    private static final Logger LOG = LoggerFactory.getLogger(PedestalCommunicationLogListener.class);

    private final CommunicationLogAttributeService communicationLogAttributeService;

    private final PedestalDao pedestalDao;

    private final PedestalCommunicationLogDao pedestalCommunicationLogDao;

    private final ObjectMapper objectMapper;

    protected PedestalCommunicationLogListener(EventService eventService, EventTaskExecutorFactory executorFactory,
                                               CommunicationLogAttributeService communicationLogAttributeService,
                                               PedestalDao pedestalDao,
                                               PedestalCommunicationLogDao pedestalCommunicationLogDao,
                                               ObjectMapper objectMapper) {
        super(eventService, executorFactory);
        this.communicationLogAttributeService = communicationLogAttributeService;
        this.pedestalDao = pedestalDao;
        this.pedestalCommunicationLogDao = pedestalCommunicationLogDao;
        this.objectMapper = objectMapper;
    }

    protected abstract LogType getLogType();

    /**
     * Attribute of the event holding the time of the logged message.
     */
    protected abstract EventAttribute getDateAttribute();

    @Override
    public void handleEvent(final Event event) {
        execute(new EventTask(event) {
            @Override
            public void handleEvent() {
                log(getEvent());
            }
        });
    }

    void log(Event event) {
        String pedestalCode = (String) event.getAttribute(EventAttribute.PEDESTAL_CODE);
        if (pedestalCode == null) {
            LOG.warn(String.format("%s without pedestal code not logged", event));
            return;
        }
        if (!communicationLogAttributeService.isPedestalLoggable(pedestalCode)) {
            LOG.info("Pedestal " + pedestalCode + " is not loggable.");
            return;
        }
        Pedestal pedestal = pedestalDao.findByCode(pedestalCode);
        if (pedestal == null) {
            LOG.error("Can't find pedestal with code " + pedestalCode);
            return;
        }
        LocalDateTime date = event.getAttribute(getDateAttribute()) instanceof LocalDateTime at
                ? at : LocalDateTime.now();
        String message = "";
        try {
            message = objectMapper.writeValueAsString(event.getAttributes());
        } catch (JsonProcessingException e) {
            LOG.error("Can't serialize the event attributes:", e);
        }
        pedestalCommunicationLogDao.save(new PedestalCommunicationLog(pedestal, date, getLogType(), message));
    }

    @Component
    public static class Boot extends PedestalCommunicationLogListener {

        public Boot(EventService eventService, EventTaskExecutorFactory executorFactory,
                    CommunicationLogAttributeService communicationLogAttributeService, PedestalDao pedestalDao,
                    PedestalCommunicationLogDao pedestalCommunicationLogDao, ObjectMapper objectMapper) {
            super(eventService, executorFactory, communicationLogAttributeService, pedestalDao,
                    pedestalCommunicationLogDao, objectMapper);
        }

        @Override
        public String getName() {
            return "bootCommunicationLogListener";
        }

        @Override
        public EventType getType() {
            return EventType.BOOT_NOTIFICATION;
        }

        @Override
        protected LogType getLogType() {
            return LogType.BOOT;
        }

        @Override
        protected EventAttribute getDateAttribute() {
            return EventAttribute.TIME;
        }
    }

    @Component
    public static class ConsumptionRecord extends PedestalCommunicationLogListener {

        public ConsumptionRecord(EventService eventService, EventTaskExecutorFactory executorFactory,
                                 CommunicationLogAttributeService communicationLogAttributeService,
                                 PedestalDao pedestalDao, PedestalCommunicationLogDao pedestalCommunicationLogDao,
                                 ObjectMapper objectMapper) {
            super(eventService, executorFactory, communicationLogAttributeService, pedestalDao,
                    pedestalCommunicationLogDao, objectMapper);
        }

        @Override
        public String getName() {
            return "consumptionRecordCommunicationLogListener";
        }

        @Override
        public EventType getType() {
            return EventType.SUPPLY_STOPPED;
        }

        @Override
        protected LogType getLogType() {
            return LogType.CONSUMPTION_RECORD;
        }

        @Override
        protected EventAttribute getDateAttribute() {
            return EventAttribute.STOP_TIME;
        }
    }
}
