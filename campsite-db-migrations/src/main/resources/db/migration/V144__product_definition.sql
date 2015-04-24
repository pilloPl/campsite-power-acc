-- Product catalogue of the orders: the seasonal pitch power package (fixed fee for the season or per month with a
-- kWh package included, the kWh above the package priced by the package's tariff), the transit card and the house
-- card set of a campsite. The ERP article numbers name the lines of the bills.

CREATE SEQUENCE PRODUCT_DEFINITION_ID_SEQ START WITH 1000 INCREMENT BY 1;

CREATE TABLE PRODUCT_DEFINITION
(
    ID                         BIGINT         NOT NULL DEFAULT nextval('PRODUCT_DEFINITION_ID_SEQ'),
    CODE                       VARCHAR(32)    NOT NULL,
    PRODUCT_TYPE               VARCHAR(32)    NOT NULL,
    MARKET                     VARCHAR(8)     NOT NULL,
    TITLE                      VARCHAR(128)   NOT NULL,
    DESCRIPTION                VARCHAR(1000),
    DURATION_MONTHS            INTEGER,
    TERMINATION_NOTICE_DAYS    INTEGER,
    HAS_BILLING                BOOLEAN        NOT NULL DEFAULT TRUE,
    HAS_FIXED_PRICE            BOOLEAN        NOT NULL DEFAULT FALSE,
    FIXED_PRICE                NUMERIC(19, 2),
    FIXED_PRICE_PERIOD         VARCHAR(16),
    FIXED_PRICE_ERP_ARTICLE_NO VARCHAR(32),
    HAS_UNIT_PRICE             BOOLEAN        NOT NULL DEFAULT FALSE,
    UNIT_PRICE_ERP_ARTICLE_NO  VARCHAR(32),
    PACKAGE_KWH                NUMERIC(19, 4),
    TARIFF_ID                  BIGINT,
    CURRENCY                   VARCHAR(3)     NOT NULL DEFAULT 'EUR',
    DELETED                    BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT PK_PRODUCT_DEFINITION PRIMARY KEY (ID),
    CONSTRAINT UNIQUE_PRODUCT_DEFINITION_CODE UNIQUE (CODE),
    CONSTRAINT FK_PRODUCT_DEFINITION_TARIFF FOREIGN KEY (TARIFF_ID) REFERENCES TARIFF (ID),
    CONSTRAINT CHECK_PRODUCT_DEFINITION_TYPE CHECK (PRODUCT_TYPE IN ('SEASONAL_POWER_PACKAGE', 'TRANSIT_CARD',
        'HOUSE_CARD_SET')),
    CONSTRAINT CHECK_PRODUCT_DEFINITION_MARKET CHECK (MARKET IN ('HR', 'IT', 'FR')),
    CONSTRAINT CHECK_PRODUCT_DEFINITION_PERIOD CHECK (FIXED_PRICE_PERIOD IS NULL
        OR FIXED_PRICE_PERIOD IN ('ONCE', 'MONTHLY', 'SEASONAL')),
    CONSTRAINT CHECK_PRODUCT_DEFINITION_FIXED CHECK (HAS_FIXED_PRICE = FALSE
        OR (FIXED_PRICE IS NOT NULL AND FIXED_PRICE >= 0 AND FIXED_PRICE_PERIOD IS NOT NULL)),
    CONSTRAINT CHECK_PRODUCT_DEFINITION_UNIT CHECK (HAS_UNIT_PRICE = FALSE OR TARIFF_ID IS NOT NULL)
);

-- tariff of the kWh above the seasonal package (not a market default)
INSERT INTO TARIFF (ID, NAME, DESCRIPTION, MARKET, CURRENCY, DEFAULT_TARIFF, LUD) VALUES
    (4, 'HR SEASONAL', 'Power above the package of the HR seasonal residents', 'HR', 'EUR', FALSE,
     TIMESTAMP '2026-01-01 00:00:00');

INSERT INTO TARIFF_ELEMENT (ID, TARIFF_ID, POS) VALUES
    (4, 4, 0);

INSERT INTO PRICE_COMPONENT (ID, TARIFF_ELEMENT_ID, DIMENSION_TYPE, PRICE, STEP_SIZE) VALUES
    (4, 4, 'ENERGY', 0.3800, 1);

INSERT INTO PRODUCT_DEFINITION (ID, CODE, PRODUCT_TYPE, MARKET, TITLE, DESCRIPTION, DURATION_MONTHS,
                                TERMINATION_NOTICE_DAYS, HAS_BILLING, HAS_FIXED_PRICE, FIXED_PRICE, FIXED_PRICE_PERIOD,
                                FIXED_PRICE_ERP_ARTICLE_NO, HAS_UNIT_PRICE, UNIT_PRICE_ERP_ARTICLE_NO, PACKAGE_KWH,
                                TARIFF_ID) VALUES
    (1, 'HR-SEASON-PACKAGE', 'SEASONAL_POWER_PACKAGE', 'HR', 'Seasonal pitch power package',
     'Fixed pitch for the season with 600 kWh included; power above the package at the seasonal tariff', 6, 30,
     TRUE, TRUE, 390.00, 'SEASONAL', 'ART-SEASON-FIX', TRUE, 'ART-SEASON-KWH', 600, 4),
    (2, 'HR-TRANSIT-CARD', 'TRANSIT_CARD', 'HR', 'Transit card',
     'Camping card for the stays of a transit guest, power billed to the reception bill', NULL, NULL,
     TRUE, TRUE, 5.00, 'ONCE', 'ART-CARD-DEPOSIT', FALSE, NULL, NULL, NULL),
    (3, 'HR-HOUSE-CARD-SET', 'HOUSE_CARD_SET', 'HR', 'House card set',
     'Staff and rental house cards for a campsite', NULL, NULL,
     FALSE, TRUE, 25.00, 'ONCE', 'ART-HOUSE-CARDS', FALSE, NULL, NULL, NULL);
