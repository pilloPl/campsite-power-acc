-- Consumption records imported from issuer partners: network guests supplied at a partner's externally managed
-- campsite. They have no supply session of the network, so SUPPLY_SESSION_ID becomes nullable. UNIQUE
-- (SUPPLY_SESSION_ID) stays: a session of the network still has one record (NULLs are distinct). An imported record
-- carries the partner (SOURCE_NAME) and the partner's record number instead; a partner's record is imported once.

ALTER TABLE CONSUMPTION_RECORD
    ALTER COLUMN SUPPLY_SESSION_ID DROP NOT NULL,
    ADD COLUMN SOURCE_NAME        VARCHAR(100),
    ADD COLUMN EXTERNAL_RECORD_NO VARCHAR(64),
    ADD CONSTRAINT UNIQUE_CONSUMPTION_RECORD_EXTERNAL UNIQUE (SOURCE_NAME, EXTERNAL_RECORD_NO),
    ADD CONSTRAINT CHECK_CONSUMPTION_RECORD_ORIGIN CHECK (
        (SUPPLY_SESSION_ID IS NOT NULL AND SOURCE_NAME IS NULL AND EXTERNAL_RECORD_NO IS NULL)
        OR (SUPPLY_SESSION_ID IS NULL AND SOURCE_NAME IS NOT NULL AND EXTERNAL_RECORD_NO IS NOT NULL));
