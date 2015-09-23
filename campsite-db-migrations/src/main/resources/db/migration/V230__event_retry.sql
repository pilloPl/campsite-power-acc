-- Events whose send failed (the event queue refused them or the send threw). The event is kept with its type and
-- its attributes (java.util.Properties text, one KEY=value per attribute) and sent again by EventRetryJob until it
-- goes through (SENT) or the attempts from SYSTEM_PROPERTY are used up (FAILED).

CREATE SEQUENCE EVENT_RETRY_ID_SEQ START WITH 1000 INCREMENT BY 1;

CREATE TABLE EVENT_RETRY
(
    ID              BIGINT        NOT NULL DEFAULT nextval('EVENT_RETRY_ID_SEQ'),
    EVENT_ID        VARCHAR(64)   NOT NULL,
    EVENT_TYPE      VARCHAR(64)   NOT NULL,
    ATTRIBUTES      TEXT          NOT NULL,
    FAILURE         VARCHAR(1024),
    STATUS          VARCHAR(16)   NOT NULL DEFAULT 'OPEN',
    RETRY_COUNT     INTEGER       NOT NULL DEFAULT 0,
    CREATE_DATE     TIMESTAMP     NOT NULL,
    LAST_RETRY_DATE TIMESTAMP,
    CONSTRAINT PK_EVENT_RETRY PRIMARY KEY (ID),
    CONSTRAINT CHECK_EVENT_RETRY_STATUS CHECK (STATUS IN ('OPEN', 'SENT', 'FAILED')),
    CONSTRAINT CHECK_EVENT_RETRY_COUNT CHECK (RETRY_COUNT >= 0)
);

CREATE INDEX IDX_EVENT_RETRY_STATUS ON EVENT_RETRY (STATUS, ID);
