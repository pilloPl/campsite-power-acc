-- Market ES (Spain): the roots and the named groups of a market (as V3 and V23 for HR, IT and FR), tariffs and
-- product definitions may be of the market, and its default tariff. Seed ids below the sequence start (1000).
INSERT INTO TBL_GROUP (ID, PARENT_ID, NAME, TYPENAME, DISPLAYNAME) VALUES
    (4, NULL, 'ES ISSUERS', 'ISSUER', 'Spain card issuers'),
    (14, NULL, 'ES SITES', 'SITE', 'Spain campsites'),
    (24, NULL, 'ES STAFF', 'STAFF', 'Spain staff'),
    (44, 4, 'ES WELCOME_CARD_POOL', 'ISSUER', 'Spain welcome card pool'),
    (54, 14, 'NETWORK ACCESS ES', 'SITE', 'Network access Spain'),
    (64, 14, 'HOUSE CARDS ONLY ES', 'SITE', 'House cards only Spain'),
    (74, 24, 'ES RECEPTION', 'STAFF', 'Spain reception'),
    (84, 24, 'ES SELF SERVICE', 'STAFF', 'Spain self-service users');

ALTER TABLE TARIFF DROP CONSTRAINT CHECK_TARIFF_MARKET;
ALTER TABLE TARIFF ADD CONSTRAINT CHECK_TARIFF_MARKET CHECK (MARKET IN ('HR', 'IT', 'FR', 'ES'));
ALTER TABLE PRODUCT_DEFINITION DROP CONSTRAINT CHECK_PRODUCT_DEFINITION_MARKET;
ALTER TABLE PRODUCT_DEFINITION ADD CONSTRAINT CHECK_PRODUCT_DEFINITION_MARKET CHECK (MARKET IN ('HR', 'IT', 'FR', 'ES'));

INSERT INTO TARIFF (ID, NAME, DESCRIPTION, MARKET, CURRENCY, DEFAULT_TARIFF, LUD) VALUES
    (5, 'ES STANDARD', 'Standard power price of the ES campsites', 'ES', 'EUR', TRUE, TIMESTAMP '2026-01-01 00:00:00');
INSERT INTO TARIFF_ELEMENT (ID, TARIFF_ID, POS) VALUES
    (5, 5, 0);
INSERT INTO PRICE_COMPONENT (ID, TARIFF_ELEMENT_ID, DIMENSION_TYPE, PRICE, STEP_SIZE) VALUES
    (5, 5, 'ENERGY', 0.4200, 1);
