-- Issuer partner: a card issuer (caravan club, camping card organisation) or an operator of externally managed
-- campsites the network exchanges with. ISSUER_GROUP_ID is the ISSUER group of the partner's cards: imported cards
-- get their guest there, and the consumption records of these guests are exported to the partner. SITE_GROUP_ID is
-- the SITE group of the partner's own campsites (pedestals MANAGED_EXTERNALLY): the network's card list goes there
-- and the partner's consumption records are accepted only from pedestals of that subtree.
--
-- INTERFACE_VERSION picks the IssuerExchange: V1 exchanges CSV files in the directory ENDPOINT (synchronised with
-- the partner by SFTP outside the application), V2 calls the REST interface of the partner at the URL ENDPOINT.
-- The four flags switch the directions of the exchange on and off per partner.

CREATE SEQUENCE ISSUER_PARTNER_ID_SEQ START WITH 1000 INCREMENT BY 1;

CREATE TABLE ISSUER_PARTNER
(
    ID                BIGINT       NOT NULL DEFAULT nextval('ISSUER_PARTNER_ID_SEQ'),
    NAME              VARCHAR(100) NOT NULL,
    DISPLAY_NAME      VARCHAR(255),
    ISSUER_GROUP_ID   BIGINT       NOT NULL,
    SITE_GROUP_ID     BIGINT,
    INTERFACE_VERSION VARCHAR(8)   NOT NULL,
    ENDPOINT          VARCHAR(500) NOT NULL,
    CARD_IMPORT       BOOLEAN      NOT NULL DEFAULT FALSE,
    CARD_EXPORT       BOOLEAN      NOT NULL DEFAULT FALSE,
    RECORD_EXPORT     BOOLEAN      NOT NULL DEFAULT FALSE,
    RECORD_IMPORT     BOOLEAN      NOT NULL DEFAULT FALSE,
    ACTIVE            BOOLEAN      NOT NULL DEFAULT TRUE,
    DELETED           BOOLEAN      NOT NULL DEFAULT FALSE,
    CREATE_DATE       TIMESTAMP    NOT NULL,
    CONSTRAINT PK_ISSUER_PARTNER PRIMARY KEY (ID),
    CONSTRAINT UNIQUE_ISSUER_PARTNER_NAME UNIQUE (NAME),
    CONSTRAINT FK_ISSUER_PARTNER_ISSUER_GROUP FOREIGN KEY (ISSUER_GROUP_ID) REFERENCES TBL_GROUP (ID),
    CONSTRAINT FK_ISSUER_PARTNER_SITE_GROUP FOREIGN KEY (SITE_GROUP_ID) REFERENCES TBL_GROUP (ID),
    CONSTRAINT CHECK_ISSUER_PARTNER_INTERFACE CHECK (INTERFACE_VERSION IN ('V1', 'V2')),
    -- cards go to and records come from the partner's own campsites
    CONSTRAINT CHECK_ISSUER_PARTNER_SITE_GROUP CHECK (SITE_GROUP_ID IS NOT NULL
        OR (CARD_EXPORT = FALSE AND RECORD_IMPORT = FALSE))
);

CREATE INDEX IDX_ISSUER_PARTNER_ISSUER_GROUP ON ISSUER_PARTNER (ISSUER_GROUP_ID);
