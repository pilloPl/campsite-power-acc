-- Address of the pedestal controller for messages of the pedestal backend (local card lists). Pedestals without an
-- address only send; lists for them cannot be delivered.

ALTER TABLE PEDESTAL ADD COLUMN LINK_URL VARCHAR(255);
