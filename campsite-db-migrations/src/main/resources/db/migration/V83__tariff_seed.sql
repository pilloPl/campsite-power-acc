-- Default tariff of every market: the last step of the tariff resolution. Energy per kWh in 1 Wh steps.
-- Seed ids below the sequence start (1000).

INSERT INTO TARIFF (ID, NAME, DESCRIPTION, MARKET, CURRENCY, DEFAULT_TARIFF, LUD) VALUES
    (1, 'HR STANDARD', 'Standard power price of the HR campsites', 'HR', 'EUR', TRUE, TIMESTAMP '2026-01-01 00:00:00'),
    (2, 'IT STANDARD', 'Standard power price of the IT campsites', 'IT', 'EUR', TRUE, TIMESTAMP '2026-01-01 00:00:00'),
    (3, 'FR STANDARD', 'Standard power price of the FR campsites', 'FR', 'EUR', TRUE, TIMESTAMP '2026-01-01 00:00:00');

INSERT INTO TARIFF_ELEMENT (ID, TARIFF_ID, POS) VALUES
    (1, 1, 0),
    (2, 2, 0),
    (3, 3, 0);

INSERT INTO PRICE_COMPONENT (ID, TARIFF_ELEMENT_ID, DIMENSION_TYPE, PRICE, STEP_SIZE) VALUES
    (1, 1, 'ENERGY', 0.4500, 1),
    (2, 2, 'ENERGY', 0.5500, 1),
    (3, 3, 'ENERGY', 0.5000, 1);
