-- ERP billing lines: what the monthly billing run hands to the ERP, one row per billing account and ERP article of
-- a billing period ("2026_07"). The lines of a seasonal contract are the fixed fee (its share of the period) and
-- the kWh above the package (priced by the contract's tariff); the power of the other supplies (transit stays,
-- pitches without a contract) is billed with the cost of its consumption records. The ERP constants of the run
-- (tax class, sales office, ...) are copied to every line, so a later change of the configuration does not change
-- lines already handed over.
--
-- EXPORT_STATUS: NEW (created by the run), TRANSFERRED (written to the ERP file), PROCESSED (the ERP booked it),
-- ERROR (the ERP refused it, ERROR_TEXT says why; sent again after a resend).

CREATE SEQUENCE ERP_BILLING_LINE_ID_SEQ START WITH 1000 INCREMENT BY 1;

CREATE TABLE ERP_BILLING_LINE
(
    ID                     BIGINT         NOT NULL DEFAULT nextval('ERP_BILLING_LINE_ID_SEQ'),
    BILLING_PERIOD         VARCHAR(7)     NOT NULL,
    BILLING_ACCOUNT_ID     BIGINT         NOT NULL,
    BILLING_ACCOUNT_NO     VARCHAR(40)    NOT NULL,
    ERP_PARTNER_NO         VARCHAR(32),
    ORDER_NUMBER           VARCHAR(32),
    LINE_TYPE              VARCHAR(16)    NOT NULL,
    ARTICLE_NO             VARCHAR(32)    NOT NULL,
    DESCRIPTION            VARCHAR(255),
    QUANTITY               NUMERIC(19, 4) NOT NULL,
    UNIT_PRICE             NUMERIC(19, 4) NOT NULL,
    AMOUNT                 NUMERIC(12, 2) NOT NULL,
    CURRENCY               VARCHAR(3)     NOT NULL,
    TAX_CLASS              VARCHAR(16),
    ARTICLE_CLASSIFICATION VARCHAR(16),
    SALES_OFFICE           VARCHAR(16),
    SALES_GROUP            VARCHAR(16),
    ASSIGNEE               VARCHAR(32),
    ORDER_TYPE             VARCHAR(16),
    ALTERNATE_TEXT         VARCHAR(255),
    EXPORT_STATUS          VARCHAR(16)    NOT NULL DEFAULT 'NEW',
    ERROR_TEXT             VARCHAR(1000),
    CREATE_DATE            TIMESTAMP      NOT NULL,
    EXPORT_DATE            TIMESTAMP,
    ANSWER_DATE            TIMESTAMP,
    CONSTRAINT PK_ERP_BILLING_LINE PRIMARY KEY (ID),
    CONSTRAINT FK_ERP_BILLING_LINE_ACCOUNT FOREIGN KEY (BILLING_ACCOUNT_ID) REFERENCES BILLING_ACCOUNT (ID),
    CONSTRAINT UNIQUE_ERP_BILLING_LINE_ARTICLE UNIQUE (BILLING_PERIOD, BILLING_ACCOUNT_NO, ARTICLE_NO),
    CONSTRAINT CHECK_ERP_BILLING_LINE_PERIOD CHECK (BILLING_PERIOD ~ '^[0-9]{4}_(0[1-9]|1[0-2])$'),
    CONSTRAINT CHECK_ERP_BILLING_LINE_TYPE CHECK (LINE_TYPE IN ('FIXED_FEE', 'PACKAGE_OVERAGE', 'POWER')),
    CONSTRAINT CHECK_ERP_BILLING_LINE_STATUS CHECK (EXPORT_STATUS IN ('NEW', 'TRANSFERRED', 'PROCESSED', 'ERROR')),
    CONSTRAINT CHECK_ERP_BILLING_LINE_QUANTITY CHECK (QUANTITY > 0)
);

CREATE INDEX IDX_ERP_BILLING_LINE_STATUS ON ERP_BILLING_LINE (EXPORT_STATUS, BILLING_PERIOD);
