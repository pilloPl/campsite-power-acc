-- Consumption record: the one settlement record of a supply session, written when the session ends (stop of the
-- pedestal or the job closing stale sessions). It is a snapshot: pedestal, socket and pitch, address of the
-- campsite, guest and billing account, the operator (site tenant) and the card issuer (issuer tenant), the
-- settlement flags, meter values, duration, nights and the price. Later changes of the infrastructure or of the
-- guest do not change a record.
--
-- RECORD_NO is the natural id <campsite code>-<yyyyMMdd of plug-out>-<number>; the number comes from a sequence
-- and is never cut to a fixed width.
--
-- Suspect records (negative duration, negative consumption, more kWh per night than the configured threshold)
-- stay in this table with VALIDATION_STATE SUSPECT and EXPORT_STATUS REVIEW until someone confirms or rejects them.
-- Records whose price could not be calculated (PRICING_STATUS PRICING_FAILED, COST null) wait in REVIEW as well.

CREATE SEQUENCE CONSUMPTION_RECORD_ID_SEQ START WITH 1000 INCREMENT BY 1;

CREATE SEQUENCE CONSUMPTION_RECORD_NO_SEQ START WITH 1 INCREMENT BY 1;

CREATE TABLE CONSUMPTION_RECORD
(
    ID                     BIGINT         NOT NULL DEFAULT nextval('CONSUMPTION_RECORD_ID_SEQ'),
    RECORD_NO              VARCHAR(64)    NOT NULL,
    SUPPLY_SESSION_ID      BIGINT         NOT NULL,
    TRANSACTION_ID         INTEGER,
    -- infrastructure snapshot
    SOCKET_ID              BIGINT,
    SOCKET_CODE            VARCHAR(64),
    SOCKET_NAME            VARCHAR(128),
    PITCH_NO               VARCHAR(16),
    PEDESTAL_ID            BIGINT,
    PEDESTAL_CODE          VARCHAR(128),
    PEDESTAL_REFERENCE_ID  VARCHAR(50),
    PEDESTAL_TYPE          VARCHAR(128),
    SITE_GROUP_ID          BIGINT,
    SITE_TENANT            VARCHAR(255),
    -- campsite address snapshot (N/A when unknown)
    CAMPSITE_CODE          VARCHAR(32),
    CAMPSITE_NAME          VARCHAR(256),
    CITY                   VARCHAR(128),
    ZIPCODE                VARCHAR(32),
    STREET                 VARCHAR(255),
    HOUSE_NUMBER           VARCHAR(32),
    COUNTRY_ISO            VARCHAR(8),
    -- card and guest snapshot
    CARD_ID                BIGINT,
    CARD_RFID              VARCHAR(64),
    CARD_NUMBER            VARCHAR(64),
    HOUSECARD_ID           BIGINT,
    HOUSE_CARD_TYPE        VARCHAR(16),
    GUEST_ID               BIGINT,
    GUEST_NAME             VARCHAR(255),
    BILLING_ACCOUNT_NO     VARCHAR(64),
    ERP_PARTNER_NO         VARCHAR(64),
    ISSUER_GROUP_ID        BIGINT,
    ISSUER_TENANT          VARCHAR(255),
    STAY_ID                BIGINT,
    UNIT_PLATE             VARCHAR(32),
    PAYMENT_CHANNEL        VARCHAR(16)    NOT NULL,
    -- settlement flags
    BILLABLE               BOOLEAN        NOT NULL,
    SETTLED_BY_CAMPSITE    BOOLEAN        NOT NULL,
    HOUSE_CARD_RENTAL      BOOLEAN        NOT NULL,
    -- supply
    START_METER_KWH        NUMERIC(19, 4),
    STOP_METER_KWH         NUMERIC(19, 4),
    KWH                    NUMERIC(19, 4),
    PLUG_IN                TIMESTAMP,
    PLUG_OUT               TIMESTAMP      NOT NULL,
    DURATION_S             BIGINT,
    NIGHTS                 INTEGER        NOT NULL,
    MAX_CURRENT_A          INTEGER,
    STOP_REASON            VARCHAR(32),
    -- price
    COST                   NUMERIC(12, 2),
    CURRENCY               VARCHAR(3),
    PRICING_STATUS         VARCHAR(16),
    PRICING_FAILURE_REASON VARCHAR(500),
    TARIFF_ID              BIGINT,
    TARIFF_NAME            VARCHAR(255),
    -- validation and export
    VALIDATION_STATE       VARCHAR(16)    NOT NULL,
    SUSPECT_REASON         VARCHAR(255),
    REVIEWED_BY            VARCHAR(255),
    REVIEW_DATE            TIMESTAMP,
    REVIEW_COMMENT         VARCHAR(1000),
    EXPORT_STATUS          VARCHAR(16)    NOT NULL,
    CREATE_DATE            TIMESTAMP      NOT NULL,
    CONSTRAINT PK_CONSUMPTION_RECORD PRIMARY KEY (ID),
    CONSTRAINT UNIQUE_CONSUMPTION_RECORD_NO UNIQUE (RECORD_NO),
    CONSTRAINT UNIQUE_CONSUMPTION_RECORD_SESSION UNIQUE (SUPPLY_SESSION_ID),
    CONSTRAINT FK_CONSUMPTION_RECORD_SESSION FOREIGN KEY (SUPPLY_SESSION_ID) REFERENCES SUPPLY_SESSION (ID),
    CONSTRAINT CHECK_CONSUMPTION_RECORD_ONE_CARD CHECK (CARD_ID IS NULL OR HOUSECARD_ID IS NULL),
    CONSTRAINT CHECK_CONSUMPTION_RECORD_PAYMENT_CHANNEL CHECK (PAYMENT_CHANNEL IN ('CARD', 'HOUSE_CARD', 'QR_APP')),
    CONSTRAINT CHECK_CONSUMPTION_RECORD_PRICING_STATUS CHECK (PRICING_STATUS IN ('PRICED', 'PRICING_FAILED',
        'NOT_BILLABLE')),
    CONSTRAINT CHECK_CONSUMPTION_RECORD_COST CHECK (COST IS NULL OR PRICING_STATUS <> 'PRICING_FAILED'),
    CONSTRAINT CHECK_CONSUMPTION_RECORD_VALIDATION_STATE CHECK (VALIDATION_STATE IN ('VALID', 'SUSPECT',
        'CONFIRMED', 'REJECTED')),
    CONSTRAINT CHECK_CONSUMPTION_RECORD_EXPORT_STATUS CHECK (EXPORT_STATUS IN ('NEW', 'TRANSFERRED', 'PROCESSED',
        'WORK', 'ERROR', 'IGNORED', 'DUPLICATE', 'REVIEW'))
);

CREATE INDEX IDX_CONSUMPTION_RECORD_PLUG_OUT ON CONSUMPTION_RECORD (PLUG_OUT);
CREATE INDEX IDX_CONSUMPTION_RECORD_SITE_GROUP ON CONSUMPTION_RECORD (SITE_GROUP_ID, PLUG_OUT);
CREATE INDEX IDX_CONSUMPTION_RECORD_ISSUER_GROUP ON CONSUMPTION_RECORD (ISSUER_GROUP_ID, PLUG_OUT);
CREATE INDEX IDX_CONSUMPTION_RECORD_CARD ON CONSUMPTION_RECORD (CARD_ID);
CREATE INDEX IDX_CONSUMPTION_RECORD_EXPORT ON CONSUMPTION_RECORD (EXPORT_STATUS);
