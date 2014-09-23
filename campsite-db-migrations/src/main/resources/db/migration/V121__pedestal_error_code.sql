-- Error masks of the pedestal controllers. A status message carries an error code; its MASK is taken from the
-- manufacturer of the pedestal type, otherwise from the link protocol rows (MANUFACTURER = 'DEFAULT').
-- The error flags of a socket are read back into abbreviations for the service mail: a MASKED row matches when all
-- its bits are set, an unmasked row only on equal flags.

CREATE SEQUENCE PEDESTAL_ERROR_CODE_ID_SEQ START WITH 1000 INCREMENT BY 1;

CREATE TABLE PEDESTAL_ERROR_CODE
(
    ID           BIGINT       NOT NULL DEFAULT nextval('PEDESTAL_ERROR_CODE_ID_SEQ'),
    MANUFACTURER VARCHAR(128) NOT NULL,
    CODE         VARCHAR(64)  NOT NULL,
    MASK         BIGINT       NOT NULL,
    MASKED       BOOLEAN      NOT NULL DEFAULT TRUE,
    ABBREVIATION VARCHAR(16)  NOT NULL,
    DESCRIPTION  VARCHAR(256) NOT NULL,
    CONSTRAINT PK_PEDESTAL_ERROR_CODE PRIMARY KEY (ID),
    CONSTRAINT UNIQUE_PEDESTAL_ERROR_CODE UNIQUE (MANUFACTURER, CODE)
);

INSERT INTO PEDESTAL_ERROR_CODE (ID, MANUFACTURER, CODE, MASK, MASKED, ABBREVIATION, DESCRIPTION) VALUES
    (1, 'DEFAULT', 'NoError', 0, FALSE, 'OK', 'No error'),
    (2, 'DEFAULT', 'BreakerTripped', 1, TRUE, 'BRK', 'Breaker of the socket tripped'),
    (3, 'DEFAULT', 'GroundFailure', 2, TRUE, 'RCD', 'Residual current device tripped'),
    (4, 'DEFAULT', 'HighTemperature', 4, TRUE, 'TMP', 'Temperature in the pedestal too high'),
    (5, 'DEFAULT', 'PowerMeterFailure', 8, TRUE, 'MTR', 'Meter of the socket does not answer'),
    (6, 'DEFAULT', 'ReaderFailure', 16, TRUE, 'RDR', 'Card reader failure'),
    (7, 'DEFAULT', 'ModemFailure', 32, TRUE, 'MDM', 'Modem failure'),
    (8, 'DEFAULT', 'WaterIngress', 64, TRUE, 'WTR', 'Water in the socket housing'),
    (9, 'DEFAULT', 'OtherError', 128, TRUE, 'OTH', 'Other error'),
    (10, 'DEFAULT', 'UnknownError', 4611686018427387904, FALSE, 'UNK', 'Unknown error code');
