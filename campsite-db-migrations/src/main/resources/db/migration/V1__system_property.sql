-- Configuration in the database: one row per SystemPropertyKey.
-- A missing row means "use the default coded in SystemPropertyService".

CREATE SEQUENCE SYSTEM_PROPERTY_ID_SEQ START WITH 1000 INCREMENT BY 1;

CREATE TABLE SYSTEM_PROPERTY
(
    ID             BIGINT        NOT NULL DEFAULT nextval('SYSTEM_PROPERTY_ID_SEQ'),
    PROPERTY_KEY   VARCHAR(100)  NOT NULL,
    PROPERTY_VALUE VARCHAR(4000) NOT NULL,
    DESCRIPTION    VARCHAR(1000) NOT NULL,
    CONSTRAINT PK_SYSTEM_PROPERTY PRIMARY KEY (ID),
    CONSTRAINT UNIQUE_SYSTEM_PROPERTY_KEY UNIQUE (PROPERTY_KEY)
);

INSERT INTO SYSTEM_PROPERTY (ID, PROPERTY_KEY, PROPERTY_VALUE, DESCRIPTION) VALUES
    (1, 'PedestalHeartbeatTimeoutInMinutes', '15', 'Pedestal is considered offline when no heartbeat arrived for this many minutes'),
    (2, 'PedestalHeartbeatTimeoutJobIntervalInSeconds', '60', 'Interval of the pedestal heartbeat timeout job'),
    (3, 'UnknownStateTimeoutInMinutes', '180', 'Socket state turns UNKNOWN after this many minutes without a status notification'),
    (4, 'StaleSessionTimeoutInHours', '24', 'Supply session without a stop is closed with TIMEOUT after this many hours'),
    (5, 'CheckoutSessionCloseDelayInMinutes', '30', 'Supply sessions of a stay are closed this many minutes after check-out'),
    (6, 'SuspectRecordKwhPerNightThreshold', '300', 'Consumption above this many kWh per stay night marks the record as SUSPECT'),
    (7, 'LocalCardListDistributionConcurrentThreads', '10', 'Number of threads distributing local card lists to pedestals'),
    (8, 'ErpArticleTaxClass', 'A1', 'Tax class of the electricity article sent to ERP');
