-- Section switchboard of a campsite: the sockets hanging on it may together draw at most MAX_CURRENT_A.
-- ALGORITHM_BEAN_NAME names the Spring bean that checks the budget; a section without (or with an unknown)
-- algorithm is a configuration error and refuses every new supply (fail-closed).

CREATE SEQUENCE POWER_SECTION_ID_SEQ START WITH 1000 INCREMENT BY 1;

CREATE TABLE POWER_SECTION
(
    ID                  BIGINT        NOT NULL DEFAULT nextval('POWER_SECTION_ID_SEQ'),
    SECTION_NAME        VARCHAR(128)  NOT NULL,
    MAX_CURRENT_A       NUMERIC(8, 2) NOT NULL,
    ALGORITHM_BEAN_NAME VARCHAR(128),
    CONSTRAINT PK_POWER_SECTION PRIMARY KEY (ID),
    CONSTRAINT UNIQUE_POWER_SECTION_NAME UNIQUE (SECTION_NAME),
    CONSTRAINT CHECK_POWER_SECTION_MAX_CURRENT CHECK (MAX_CURRENT_A > 0)
);
