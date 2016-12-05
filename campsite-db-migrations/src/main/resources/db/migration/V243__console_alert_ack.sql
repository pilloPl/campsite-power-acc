-- Live alerts of the console dashboard are derived from the data (faulted sockets, timed-out pedestals, stale sessions,
-- the review queue, failed events, refused ERP lines); only their acknowledgement is stored. The key names the source
-- and its occurrence (e.g. SOCKET_FAULT:1017:1784012345), so a new fault of the same socket is a new alert.

CREATE SEQUENCE CONSOLE_ALERT_ACK_ID_SEQ START WITH 1000 INCREMENT BY 1;

CREATE TABLE CONSOLE_ALERT_ACK
(
    ID              BIGINT        NOT NULL DEFAULT nextval('CONSOLE_ALERT_ACK_ID_SEQ'),
    ALERT_KEY       VARCHAR(128)  NOT NULL,
    ACKNOWLEDGED_BY VARCHAR(100)  NOT NULL,
    ACK_DATE        TIMESTAMP     NOT NULL,
    ACK_COMMENT     VARCHAR(1000),
    CONSTRAINT PK_CONSOLE_ALERT_ACK PRIMARY KEY (ID),
    CONSTRAINT UNIQUE_CONSOLE_ALERT_ACK_KEY UNIQUE (ALERT_KEY)
);

CREATE INDEX IDX_CONSOLE_ALERT_ACK_DATE ON CONSOLE_ALERT_ACK (ACK_DATE);
