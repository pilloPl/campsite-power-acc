-- House cards of a campsite: its own cards (staff / service, or rental cards lent to transit guests and settled by
-- the campsite), accepted without further validation at the pedestals of the campsite's SITE subtree.
-- A group attribute with many rows per group. LAST_USED decides which house cards stay on a full local card list.

CREATE TABLE HOUSECARD
(
    ID              BIGINT      NOT NULL DEFAULT nextval('GROUP_ATTRIBUTE_ID_SEQ'),
    GROUP_ID        BIGINT      NOT NULL,
    HOUSE_CARD_TYPE VARCHAR(16) NOT NULL,
    RFID            VARCHAR(64) NOT NULL,
    CARD_NUMBER     VARCHAR(64),
    CREATION_DATE   TIMESTAMP   NOT NULL,
    LAST_USED       TIMESTAMP,
    DELETED_DATE    TIMESTAMP,
    ACTIVE          BOOLEAN     NOT NULL DEFAULT TRUE,
    DELETED         BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT PK_HOUSECARD PRIMARY KEY (ID),
    CONSTRAINT FK_HOUSECARD_GROUP FOREIGN KEY (GROUP_ID) REFERENCES TBL_GROUP (ID),
    CONSTRAINT CHECK_HOUSECARD_TYPE CHECK (HOUSE_CARD_TYPE IN ('STAFF', 'RENTAL'))
);

CREATE INDEX IDX_HOUSECARD_GROUP ON HOUSECARD (GROUP_ID);
CREATE INDEX IDX_HOUSECARD_RFID ON HOUSECARD (RFID);
