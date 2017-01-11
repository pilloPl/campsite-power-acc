-- Demo data (profile demo): what operations made of the supplies: seasonal contracts and orders, review queue, ERP
-- billing of the month before, partner export, QR payments, faults, incidents, logs, events, audit, configuration.

-- seasonal contracts of the two residents in Istria (package 600 kWh, power above it at HR SEASONAL)
INSERT INTO ORDER_HEADER (ID, ORDER_NUMBER, GROUP_ID, GUEST_ID, BILLING_ACCOUNT_ID, STATUS, GUEST_TYPE, SALUTATION,
                          NAME, FIRST_NAME, CITY, COUNTRY_ISO, LANGUAGE, EMAIL, PAYMENT_METHOD, CAR_PLATE,
                          ERP_PARTNER_NO, ERP_ACCOUNT_NO, EXPORTED, EXPORT_DATE, CREATE_DATE, CREATED_BY,
                          ACTIVATION_DATE)
SELECT g.ID, 'D' || (g.ID - 900000000), 900000001, g.ID, ba.ID, 'ACTIVE', 'SEASONAL', g.SALUTATION, g.NAME,
       g.FIRST_NAME, g.CITY, g.COUNTRY_ISO, g.LANGUAGE, g.EMAIL, g.PAYMENT_METHOD, g.CAR_PLATE, g.ERP_PARTNER_NO,
       ba.ERP_ACCOUNT_NO, TRUE, g.CREATE_DATE + INTERVAL '1 day', g.CREATE_DATE, 'reception',
       g.CREATE_DATE + INTERVAL '2 days'
  FROM GUEST g
  JOIN BILLING_ACCOUNT ba ON ba.GUEST_ID = g.ID
 WHERE g.ID >= 900000000
   AND g.GUEST_TYPE = 'SEASONAL';
INSERT INTO ORDER_ITEM (ID, ORDER_ID, POS, PRODUCT_DEFINITION_ID, TITLE, QUANTITY, FIXED_PRICE, FIXED_PRICE_PERIOD,
                        PACKAGE_KWH, SITE_GROUP_ID, PITCH_NO, START_DATE, END_DATE, CARD_ID)
SELECT oh.ID, oh.ID, 1, 1, 'Seasonal pitch power package', 1, 390.00, 'SEASONAL', 600, 900000001, cc.PITCH_NO,
       c.VALID_FROM, c.VALID_TO, c.ID
  FROM ORDER_HEADER oh
  JOIN CAMPING_CARD c ON c.GUEST_ID = oh.GUEST_ID
  JOIN LATERAL (SELECT so.PITCH_NO
                  FROM SUPPLY_SESSION ss
                  JOIN SOCKET so ON so.ID = ss.SOCKET_ID
                 WHERE ss.CARD_ID = c.ID
                 LIMIT 1) cc ON TRUE
 WHERE oh.ID >= 900000000;
INSERT INTO SEASONAL_CONTRACT (ID, BILLING_ACCOUNT_ID, ORDER_ITEM_ID, TARIFF_ID, STATUS, SITE_GROUP_ID, PITCH_NO,
                               START_DATE, END_DATE, REQUESTED_END_DATE, FIXED_FEE, FEE_PERIOD, CREATE_DATE, MOD_DATE)
SELECT oi.ID, oh.BILLING_ACCOUNT_ID, oi.ID, 4,
       CASE WHEN oi.PITCH_NO = 'B10' THEN 'TERMINATION_REQUESTED' ELSE 'ACTIVE' END, 900000001, oi.PITCH_NO,
       oi.START_DATE, oi.END_DATE, CASE WHEN oi.PITCH_NO = 'B10' THEN CURRENT_DATE + 5 END, 390.00, 'SEASONAL',
       oh.CREATE_DATE, CASE WHEN oi.PITCH_NO = 'B10' THEN now() - INTERVAL '3 days' END
  FROM ORDER_ITEM oi
  JOIN ORDER_HEADER oh ON oh.ID = oi.ORDER_ID
 WHERE oi.ID >= 900000000;
INSERT INTO PACKAGE_ITEM (ID, CONTRACT_ID, ORDER_ITEM_ID, PRODUCT_DEFINITION_ID, PACKAGE_KWH)
SELECT sc.ID, sc.ID, sc.ORDER_ITEM_ID, 1, 600
  FROM SEASONAL_CONTRACT sc
 WHERE sc.ID >= 900000000;

-- transit card orders of the latest Istria guests: new (bookings for next week), waiting for the ERP numbers,
-- active, cancelled
INSERT INTO ORDER_HEADER (ID, ORDER_NUMBER, GROUP_ID, GUEST_ID, BILLING_ACCOUNT_ID, STATUS, GUEST_TYPE, SALUTATION,
                          NAME, FIRST_NAME, CITY, COUNTRY_ISO, LANGUAGE, EMAIL, PAYMENT_METHOD, CAR_PLATE,
                          ERP_PARTNER_NO, ERP_ACCOUNT_NO, EXPORTED, CREATE_DATE, CREATED_BY, ACTIVATION_DATE)
SELECT g.ID, 'D' || (g.ID - 900000000), 31,
       CASE WHEN o.N % 4 = 0 THEN NULL ELSE g.ID END, CASE WHEN o.N % 4 = 2 THEN ba.ID END,
       (ARRAY ['NEW', 'AWAITING_ERP_ACCOUNT', 'ACTIVE', 'CANCELLED'])[1 + o.N % 4], 'TRANSIT', g.SALUTATION, g.NAME,
       g.FIRST_NAME, g.CITY, g.COUNTRY_ISO, g.LANGUAGE, g.EMAIL, 'RECEPTION', g.CAR_PLATE,
       CASE WHEN o.N % 4 = 2 THEN g.ERP_PARTNER_NO END, CASE WHEN o.N % 4 = 2 THEN ba.ERP_ACCOUNT_NO END,
       o.N % 4 = 2, now() - (o.N || ' hours')::INTERVAL, 'reception',
       CASE WHEN o.N % 4 = 2 THEN now() - (o.N || ' hours')::INTERVAL + INTERVAL '20 minutes' END
  FROM (SELECT g2.ID, ROW_NUMBER() OVER (ORDER BY g2.ID DESC) AS N
          FROM GUEST g2
         WHERE g2.ID >= 900000000 AND g2.GROUP_ID = 31 AND g2.GUEST_TYPE = 'TRANSIT'
         ORDER BY g2.ID DESC
         LIMIT 8) o
  JOIN GUEST g ON g.ID = o.ID
  JOIN BILLING_ACCOUNT ba ON ba.GUEST_ID = g.ID;
INSERT INTO ORDER_ITEM (ID, ORDER_ID, POS, PRODUCT_DEFINITION_ID, TITLE, QUANTITY, FIXED_PRICE, FIXED_PRICE_PERIOD,
                        SITE_GROUP_ID, START_DATE, END_DATE, CARD_ID)
SELECT oh.ID, oh.ID, 1, 2, 'Transit card', 1, 5.00, 'ONCE', 900000001,
       CASE WHEN oh.STATUS = 'NEW' THEN CURRENT_DATE + 6 ELSE st.ARRIVAL_DATE END,
       CASE WHEN oh.STATUS = 'NEW' THEN CURRENT_DATE + 13 ELSE st.DEPARTURE_DATE END,
       CASE WHEN oh.STATUS = 'ACTIVE' THEN st.CARD_ID END
  FROM ORDER_HEADER oh
  JOIN STAY st ON st.ID = oh.ID
 WHERE oh.ID >= 900000000
   AND oh.GUEST_TYPE = 'TRANSIT';

-- review queue: meter jumps (suspect), a supply without tariff, two reviewed ones
UPDATE CONSUMPTION_RECORD
   SET KWH              = KWH + 700 + (ID % 5) * 40,
       STOP_METER_KWH   = STOP_METER_KWH + 700 + (ID % 5) * 40,
       COST             = ROUND((KWH + 700 + (ID % 5) * 40) * 0.45, 2),
       VALIDATION_STATE = 'SUSPECT',
       SUSPECT_REASON   = 'Consumption ' || ROUND(KWH + 700 + (ID % 5) * 40, 3) || ' kWh in ' || GREATEST(NIGHTS, 1)
           || ' night(s) is above 300 kWh per night',
       EXPORT_STATUS    = 'REVIEW'
 WHERE ID IN (SELECT cr.ID
                FROM CONSUMPTION_RECORD cr
               WHERE cr.ID >= 900000000 AND cr.PAYMENT_CHANNEL = 'CARD' AND cr.NIGHTS <= 2
                 AND cr.PLUG_OUT > now() - INTERVAL '12 days'
               ORDER BY cr.ID % 13, cr.ID
               LIMIT 5);
UPDATE CONSUMPTION_RECORD
   SET COST                   = NULL,
       CURRENCY               = NULL,
       PRICING_STATUS         = 'PRICING_FAILED',
       PRICING_FAILURE_REASON = 'No tariff found for socket ' || SOCKET_CODE || ' at plug-in',
       TARIFF_ID              = NULL,
       TARIFF_NAME            = NULL,
       EXPORT_STATUS          = 'REVIEW'
 WHERE ID IN (SELECT cr.ID
                FROM CONSUMPTION_RECORD cr
               WHERE cr.ID >= 900000000 AND cr.PAYMENT_CHANNEL = 'CARD' AND cr.VALIDATION_STATE = 'VALID'
                 AND cr.PLUG_OUT > now() - INTERVAL '5 days' AND cr.SITE_GROUP_ID = 900000002
               ORDER BY cr.ID
               LIMIT 1);
UPDATE CONSUMPTION_RECORD
   SET KWH = KWH + 420, STOP_METER_KWH = STOP_METER_KWH + 420, COST = ROUND((KWH + 420) * 0.45, 2),
       VALIDATION_STATE = 'CONFIRMED', SUSPECT_REASON = 'Consumption above 300 kWh per night',
       REVIEWED_BY = 'backoffice', REVIEW_DATE = PLUG_OUT + INTERVAL '1 day',
       REVIEW_COMMENT = 'Heat pump of a large caravan, guest confirmed at the reception'
 WHERE ID = (SELECT MIN(cr.ID) FROM CONSUMPTION_RECORD cr
              WHERE cr.ID >= 900000000 AND cr.PAYMENT_CHANNEL = 'CARD' AND cr.SITE_GROUP_ID = 900000001
                AND cr.VALIDATION_STATE = 'VALID' AND cr.NIGHTS = 1 AND cr.PLUG_OUT > now() - INTERVAL '10 days');
UPDATE CONSUMPTION_RECORD
   SET KWH = KWH + 900, STOP_METER_KWH = STOP_METER_KWH + 900, VALIDATION_STATE = 'REJECTED',
       SUSPECT_REASON = 'Consumption above 300 kWh per night', REVIEWED_BY = 'backoffice',
       REVIEW_DATE = PLUG_OUT + INTERVAL '2 days', REVIEW_COMMENT = 'Meter replaced, reading wrong',
       EXPORT_STATUS = 'IGNORED'
 WHERE ID = (SELECT MIN(cr.ID) FROM CONSUMPTION_RECORD cr
              WHERE cr.ID >= 900000000 AND cr.PAYMENT_CHANNEL = 'CARD' AND cr.SITE_GROUP_ID = 900000003
                AND cr.VALIDATION_STATE = 'VALID' AND cr.PLUG_OUT > now() - INTERVAL '10 days');

-- records of the partner's members up to two days ago went to the partner; the last export failed
UPDATE CONSUMPTION_RECORD
   SET EXPORT_STATUS = 'TRANSFERRED'
 WHERE ID >= 900000000 AND ISSUER_GROUP_ID = 900000011 AND EXPORT_STATUS = 'NEW'
   AND PLUG_OUT < now() - INTERVAL '2 days';

-- ERP billing run of the month before: power lines per account, fixed fee and overage of the seasonal contracts
DROP TABLE IF EXISTS pg_temp.demo_erp;
CREATE TEMPORARY TABLE demo_erp AS
SELECT cr.BILLING_ACCOUNT_NO, ba.ID AS BILLING_ACCOUNT_ID, g.ERP_PARTNER_NO, g.GUEST_TYPE,
       SUM(cr.KWH) AS KWH, SUM(cr.COST) AS COST
  FROM CONSUMPTION_RECORD cr
  JOIN BILLING_ACCOUNT ba ON ba.ERP_ACCOUNT_NO = cr.BILLING_ACCOUNT_NO AND ba.GUEST_ID = cr.GUEST_ID
  JOIN GUEST g ON g.ID = cr.GUEST_ID
 WHERE cr.ID >= 900000000
   AND cr.PAYMENT_CHANNEL = 'CARD' AND cr.BILLABLE AND NOT cr.SETTLED_BY_CAMPSITE AND cr.PRICING_STATUS = 'PRICED'
   AND cr.VALIDATION_STATE <> 'REJECTED' AND cr.EXPORT_STATUS = 'NEW'
   AND cr.PLUG_OUT >= date_trunc('month', now()) - INTERVAL '1 month' AND cr.PLUG_OUT < date_trunc('month', now())
 GROUP BY cr.BILLING_ACCOUNT_NO, ba.ID, g.ERP_PARTNER_NO, g.GUEST_TYPE
HAVING SUM(cr.COST) >= 5.00;

INSERT INTO ERP_BILLING_LINE (ID, BILLING_PERIOD, BILLING_ACCOUNT_ID, BILLING_ACCOUNT_NO, ERP_PARTNER_NO, ORDER_NUMBER,
                              LINE_TYPE, ARTICLE_NO, DESCRIPTION, QUANTITY, UNIT_PRICE, AMOUNT, CURRENCY, TAX_CLASS,
                              ARTICLE_CLASSIFICATION, SALES_OFFICE, SALES_GROUP, ASSIGNEE, ORDER_TYPE, ALTERNATE_TEXT,
                              EXPORT_STATUS, ERROR_TEXT, CREATE_DATE, EXPORT_DATE, ANSWER_DATE)
SELECT 900000000 + ROW_NUMBER() OVER (ORDER BY e.BILLING_ACCOUNT_NO, l.LINE_TYPE),
       TO_CHAR(date_trunc('month', now()) - INTERVAL '1 month', 'YYYY_MM'), e.BILLING_ACCOUNT_ID,
       e.BILLING_ACCOUNT_NO, e.ERP_PARTNER_NO, oh.ORDER_NUMBER, l.LINE_TYPE, l.ARTICLE_NO, l.DESCRIPTION, l.QUANTITY,
       l.UNIT_PRICE, l.AMOUNT, 'EUR', 'A1', 'EL', 'CS01', 'PWR', 'CAMPSITE-POWER', 'ZPW', 'Pitch power of your stay',
       CASE WHEN e.BILLING_ACCOUNT_ID % 9 = 4 THEN 'ERROR'
            WHEN e.BILLING_ACCOUNT_ID % 9 = 7 THEN 'TRANSFERRED' ELSE 'PROCESSED' END,
       CASE WHEN e.BILLING_ACCOUNT_ID % 9 = 4 THEN
                CASE WHEN e.BILLING_ACCOUNT_ID % 2 = 0 THEN 'Account ' || e.BILLING_ACCOUNT_NO || ' blocked: dunning level 3'
                     ELSE 'Partner ' || COALESCE(e.ERP_PARTNER_NO, '?') || ' unknown in the ERP' END END,
       date_trunc('month', now()) + INTERVAL '2 days 2 hours',
       date_trunc('month', now()) + INTERVAL '2 days 3 hours',
       CASE WHEN e.BILLING_ACCOUNT_ID % 9 <> 7 THEN date_trunc('month', now()) + INTERVAL '2 days 9 hours' END
  FROM pg_temp.demo_erp e
  LEFT JOIN ORDER_HEADER oh ON oh.BILLING_ACCOUNT_ID = e.BILLING_ACCOUNT_ID AND oh.STATUS = 'ACTIVE'
  JOIN LATERAL (SELECT 'POWER' AS LINE_TYPE, 'ART-POWER-KWH' AS ARTICLE_NO, 'Pitch power' AS DESCRIPTION,
                       ROUND(e.KWH, 4) AS QUANTITY, ROUND(e.COST / NULLIF(e.KWH, 0), 4) AS UNIT_PRICE,
                       ROUND(e.COST, 2) AS AMOUNT
                 WHERE e.GUEST_TYPE = 'TRANSIT' AND e.KWH > 0
                UNION ALL
                SELECT 'FIXED_FEE', 'ART-SEASON-FIX', 'Seasonal pitch power package, share of the month', 1, 55.7143,
                       55.71
                 WHERE e.GUEST_TYPE = 'SEASONAL'
                UNION ALL
                SELECT 'PACKAGE_OVERAGE', 'ART-SEASON-KWH', 'Power above the package', ROUND(e.KWH - 100, 4), 0.3800,
                       ROUND((e.KWH - 100) * 0.38, 2)
                 WHERE e.GUEST_TYPE = 'SEASONAL' AND e.KWH > 100) l ON TRUE;

UPDATE CONSUMPTION_RECORD cr
   SET ERP_BILLING_PERIOD = l.BILLING_PERIOD,
       EXPORT_STATUS      = l.EXPORT_STATUS
  FROM (SELECT DISTINCT ON (BILLING_ACCOUNT_NO) BILLING_ACCOUNT_NO, BILLING_PERIOD, EXPORT_STATUS
          FROM ERP_BILLING_LINE
         WHERE ID >= 900000000
         ORDER BY BILLING_ACCOUNT_NO, ID) l
 WHERE cr.ID >= 900000000
   AND cr.BILLING_ACCOUNT_NO = l.BILLING_ACCOUNT_NO
   AND cr.PAYMENT_CHANNEL = 'CARD' AND cr.EXPORT_STATUS = 'NEW' AND cr.PRICING_STATUS = 'PRICED'
   AND cr.PLUG_OUT >= date_trunc('month', now()) - INTERVAL '1 month' AND cr.PLUG_OUT < date_trunc('month', now());

-- the fixed fee of the residents two months back (an earlier run)
INSERT INTO ERP_BILLING_LINE (ID, BILLING_PERIOD, BILLING_ACCOUNT_ID, BILLING_ACCOUNT_NO, ERP_PARTNER_NO, ORDER_NUMBER,
                              LINE_TYPE, ARTICLE_NO, DESCRIPTION, QUANTITY, UNIT_PRICE, AMOUNT, CURRENCY, TAX_CLASS,
                              ARTICLE_CLASSIFICATION, SALES_OFFICE, SALES_GROUP, ASSIGNEE, ORDER_TYPE, ALTERNATE_TEXT,
                              EXPORT_STATUS, CREATE_DATE, EXPORT_DATE, ANSWER_DATE)
SELECT 900005000 + ba.ID - 900000000, TO_CHAR(date_trunc('month', now()) - INTERVAL '2 months', 'YYYY_MM'), ba.ID,
       ba.ERP_ACCOUNT_NO, g.ERP_PARTNER_NO, oh.ORDER_NUMBER, 'FIXED_FEE', 'ART-SEASON-FIX',
       'Seasonal pitch power package, share of the month', 1, 55.7143, 55.71, 'EUR', 'A1', 'EL', 'CS01', 'PWR',
       'CAMPSITE-POWER', 'ZPW', 'Pitch power of your stay', 'PROCESSED',
       date_trunc('month', now()) - INTERVAL '1 month' + INTERVAL '2 days 2 hours',
       date_trunc('month', now()) - INTERVAL '1 month' + INTERVAL '2 days 3 hours',
       date_trunc('month', now()) - INTERVAL '1 month' + INTERVAL '2 days 9 hours'
  FROM BILLING_ACCOUNT ba
  JOIN GUEST g ON g.ID = ba.GUEST_ID AND g.GUEST_TYPE = 'SEASONAL'
  LEFT JOIN ORDER_HEADER oh ON oh.BILLING_ACCOUNT_ID = ba.ID
 WHERE ba.ID >= 900000000;

-- QR payments: the chain of states of every QR supply, plus one running, one stuck and one failed start
CREATE OR REPLACE FUNCTION pg_temp.demo_qr_chain(p_no BIGINT, p_socket BIGINT, p_states TEXT[], p_start TIMESTAMP,
                                                 p_stop TIMESTAMP, p_session BIGINT, p_kwh NUMERIC, p_cost NUMERIC,
                                                 p_error TEXT, p_message TEXT) RETURNS VOID
    LANGUAGE plpgsql AS
$f$
DECLARE
    v_first BIGINT := 900000000 + p_no * 12;
    v_id    BIGINT;
    v_i     INTEGER;
    v_state TEXT;
    v_at    TIMESTAMP;
    v_stop  BOOLEAN := FALSE;
BEGIN
    FOR v_i IN 1..array_length(p_states, 1) LOOP
        v_state := p_states[v_i];
        v_id := v_first + v_i - 1;
        v_stop := v_stop OR v_state = 'STOP_REQUESTED';
        v_at := CASE WHEN v_stop THEN COALESCE(p_stop, p_start) + (v_i * 4) * INTERVAL '1 second'
                     ELSE p_start - INTERVAL '40 seconds' + (v_i * 6) * INTERVAL '1 second' END;
        INSERT INTO QRPAY_PROCESS (ID, PROCESS_ID, ORDER_REFERENCE, STATE, SOCKET_ID, CLIENT_USERNAME, APP_USER_ID,
                                   REQUEST_DATE, START_TIME, STOP_TIME, PRICE_INFORMATION, SUPPLY_SESSION_ID,
                                   TRANSACTION_ID, KWH, DURATION_SECONDS, COST, CURRENCY, ERROR_CODE, ERROR_MESSAGE,
                                   PREVIOUS_PROCESS_ID, NEXT_PROCESS_ID)
        VALUES (v_id, v_first, 'QR-' || (700000 + p_no), v_state, p_socket, 'qrpay-app', 'app-user-' || (p_no % 37),
                v_at, p_start, CASE WHEN v_stop THEN p_stop END, '0.45-0.62 EUR/kWh',
                CASE WHEN v_state NOT IN ('START_REQUESTED', 'SOCKET_START_WAITING', 'ERROR') THEN p_session END,
                CASE WHEN v_state NOT IN ('START_REQUESTED', 'SOCKET_START_WAITING', 'ERROR') THEN p_session::INTEGER END,
                CASE WHEN v_state IN ('CALCULATION_DONE', 'CLOSED') THEN p_kwh END,
                CASE WHEN v_state IN ('CALCULATION_DONE', 'CLOSED') THEN EXTRACT(EPOCH FROM (p_stop - p_start))::BIGINT END,
                CASE WHEN v_state IN ('CALCULATION_DONE', 'CLOSED') THEN p_cost END,
                CASE WHEN v_state IN ('CALCULATION_DONE', 'CLOSED') THEN 'EUR' END,
                CASE WHEN v_state = 'ERROR' THEN p_error END, CASE WHEN v_state = 'ERROR' THEN p_message END,
                CASE WHEN v_i > 1 THEN v_id - 1 END, NULL);
        IF v_i > 1 THEN
            UPDATE QRPAY_PROCESS SET NEXT_PROCESS_ID = v_id WHERE ID = v_id - 1;
        END IF;
    END LOOP;
END
$f$;

SELECT pg_temp.demo_qr_chain(ROW_NUMBER() OVER (ORDER BY cr.ID), cr.SOCKET_ID,
                             ARRAY ['START_REQUESTED', 'SOCKET_START_WAITING', 'SOCKET_START_RECEIVED',
                                 'START_CONFIRMED', 'STOP_REQUESTED', 'SOCKET_STOP_WAITING', 'SOCKET_STOP_RECEIVED',
                                 'CALCULATION_REQUESTED', 'CALCULATION_DONE', 'CLOSED'],
                             cr.PLUG_IN, cr.PLUG_OUT, cr.SUPPLY_SESSION_ID, cr.KWH, cr.COST, NULL, NULL)
  FROM CONSUMPTION_RECORD cr
  JOIN pg_temp.demo_qr_session q ON q.SESSION_ID = cr.SUPPLY_SESSION_ID;

UPDATE SUPPLY_SESSION ss
   SET QRPAY_PROCESS_ID = qp.PROCESS_ID
  FROM QRPAY_PROCESS qp
 WHERE qp.SUPPLY_SESSION_ID = ss.ID AND qp.ID = qp.PROCESS_ID AND qp.ID >= 900000000;
UPDATE SUPPLY_SESSION ss
   SET QRPAY_PROCESS_ID = (SELECT qp.PROCESS_ID FROM QRPAY_PROCESS qp WHERE qp.SUPPLY_SESSION_ID = ss.ID LIMIT 1)
 WHERE ss.ID IN (SELECT SESSION_ID FROM pg_temp.demo_qr_session) AND ss.QRPAY_PROCESS_ID IS NULL;

DO
$$
    DECLARE
        v_socket  BIGINT;
        v_session BIGINT;
    BEGIN
        -- running QR supply on a free Garda socket
        v_socket := 900012041;
        BEGIN
            v_session := pg_temp.demo_supply(v_socket, NULL, NULL, NULL, NULL,
                                             (now() - INTERVAL '95 minutes')::TIMESTAMP,
                                             (now() - INTERVAL '4 minutes')::TIMESTAMP, 1.1, TRUE, NULL);
            PERFORM pg_temp.demo_qr_chain(9001, v_socket, ARRAY ['START_REQUESTED', 'SOCKET_START_WAITING',
                'SOCKET_START_RECEIVED', 'START_CONFIRMED'], (now() - INTERVAL '95 minutes')::TIMESTAMP, NULL,
                                          v_session, NULL, NULL, NULL, NULL);
            UPDATE SUPPLY_SESSION SET QRPAY_PROCESS_ID = 900000000 + 9001 * 12 WHERE ID = v_session;
        END;
        -- stuck: the pedestal never confirmed the start
        v_socket := 900013021;
        BEGIN
            PERFORM pg_temp.demo_qr_chain(9002, v_socket, ARRAY ['START_REQUESTED', 'SOCKET_START_WAITING'],
                                          (now() - INTERVAL '3 hours')::TIMESTAMP, NULL, NULL, NULL, NULL, NULL,
                                          NULL);
        END;
        -- failed start yesterday
        SELECT so.ID INTO v_socket FROM SOCKET so WHERE so.PEDESTAL_ID = 900000203 ORDER BY so.ID LIMIT 1;
        PERFORM pg_temp.demo_qr_chain(9003, v_socket, ARRAY ['START_REQUESTED', 'ERROR'],
                                      (now() - INTERVAL '26 hours')::TIMESTAMP, NULL, NULL, NULL, NULL,
                                      'REMOTE_SUPPLY_START_FAILED', 'The pedestal did not accept the start.');
    END
$$;

-- faults: a residual current device and water in a socket housing, a tripped breaker
DO
$$
    DECLARE
        f        RECORD;
        v_socket BIGINT;
        v_last   TIMESTAMP;
        v_meter  NUMERIC;
    BEGIN
        FOR f IN SELECT * FROM (VALUES (900011024::BIGINT, 'FAULTED', 2::BIGINT, INTERVAL '5 hours'),
                                       (900012054::BIGINT, 'BREAKER_TRIPPED', 1::BIGINT, INTERVAL '50 minutes'),
                                       (900013034::BIGINT, 'FAULTED', 64::BIGINT, INTERVAL '2 days'))
                                AS t(SOCKET_ID, STATUS, FLAGS, AGO)
        LOOP
            SELECT so.ID, so.MOD_DATE, so.METER_KWH INTO v_socket, v_last, v_meter
              FROM SOCKET so
             WHERE so.ID = f.SOCKET_ID;
            v_last := GREATEST(v_last + INTERVAL '1 minute', now() - f.AGO);
            INSERT INTO SOCKET_STATUS_LOG (ID, SOCKET_ID, CREATE_DATE, UPDATE_DATE, REMOTE_DATE, STATUS, METER_KWH,
                                           ERROR_FLAGS, CURRENT_A)
            VALUES (nextval('pg_temp.demo_log_seq'), v_socket, v_last, v_last, v_last, f.STATUS, v_meter, f.FLAGS, 0);
            UPDATE SOCKET SET STATUS = f.STATUS, MOD_DATE = v_last, LAST_STATUS_UPDATE = v_last WHERE ID = v_socket;
        END LOOP;
    END
$$;

-- incidents of the pedestal link
INSERT INTO SUPPLY_INCIDENT_LOG (ID, INCIDENT_TYPE, PEDESTAL_CODE, TRANSACTION_ID, CARD_RFID, SUPPLY_SESSION_ID,
                                 METER_KWH, REMOTE_DATE, MESSAGE, CREATE_DATE)
SELECT 900000000 + i.N, i.TYPE, p.CODE, CASE WHEN i.TYPE = 'NO_TRANSACTION_ID' THEN NULL ELSE ss.TRANSACTION_ID END,
       cc.CARD_RFID, CASE WHEN i.TYPE = 'SESSION_ALREADY_CLOSED' THEN ss.ID END, ss.START_METER_KWH + 3.2,
       now() - (i.N * 31 || ' hours')::INTERVAL,
       CASE i.TYPE WHEN 'NO_SESSION' THEN 'Supply stop for transaction ' || ss.TRANSACTION_ID || ' without a session'
                   WHEN 'SESSION_ALREADY_CLOSED' THEN 'Meter reading after the stop of transaction '
                       || ss.TRANSACTION_ID
                   ELSE 'Meter reading without transaction id on socket ' || so.SOCKET_NO END,
       now() - (i.N * 31 || ' hours')::INTERVAL
  FROM (VALUES (1, 'NO_SESSION'), (2, 'SESSION_ALREADY_CLOSED'), (3, 'NO_TRANSACTION_ID'), (4, 'SESSION_ALREADY_CLOSED'),
               (5, 'NO_SESSION'), (6, 'NO_TRANSACTION_ID'), (7, 'SESSION_ALREADY_CLOSED')) AS i(N, TYPE)
  JOIN LATERAL (SELECT s2.* FROM SUPPLY_SESSION s2
                  JOIN SOCKET so2 ON so2.ID = s2.SOCKET_ID
                  JOIN PEDESTAL p2 ON p2.ID = so2.PEDESTAL_ID
                 WHERE s2.ID >= 900000000 AND s2.CARD_ID IS NOT NULL AND NOT s2.ACTIVE
                   AND p2.GROUP_ID = 900000001 + i.N % 3 -- incidents on all three campsites
                 ORDER BY s2.ID DESC OFFSET i.N * 5 LIMIT 1) ss ON TRUE
  JOIN SOCKET so ON so.ID = ss.SOCKET_ID
  JOIN PEDESTAL p ON p.ID = so.PEDESTAL_ID
  LEFT JOIN CAMPING_CARD cc ON cc.ID = ss.CARD_ID;

-- communication log: boots of every pedestal, received records of the logged campsites
INSERT INTO PEDESTAL_COMM_LOG (ID, PEDESTAL_ID, LOG_DATE, LOG_TYPE, MESSAGE)
SELECT 900000000 + p.ID % 1000 * 10 + b.N, p.ID, now() - (b.DAYS || ' days')::INTERVAL - (p.ID % 60) * INTERVAL '1 minute',
       'BOOT', 'Boot of ' || p.CODE || ': ' || pt.MANUFACTURER || ' ' || pt.NAME || ', firmware ' || p.FIRMWARE_VERSION
           || ', serial ' || p.SERIAL_NUMBER
  FROM PEDESTAL p
  JOIN PEDESTAL_TYPE pt ON pt.ID = p.PEDESTAL_TYPE_ID,
       (VALUES (1, 41), (2, 12), (3, 2)) AS b(N, DAYS)
 WHERE p.ID >= 900000000;
INSERT INTO PEDESTAL_COMM_LOG (ID, PEDESTAL_ID, LOG_DATE, LOG_TYPE, MESSAGE)
SELECT 900100000 + ROW_NUMBER() OVER (ORDER BY cr.ID), cr.PEDESTAL_ID, cr.CREATE_DATE, 'CONSUMPTION_RECORD',
       'Record ' || cr.RECORD_NO || ': ' || ROUND(cr.KWH, 3) || ' kWh, transaction ' || cr.TRANSACTION_ID
  FROM CONSUMPTION_RECORD cr
 WHERE cr.ID >= 900000000 AND cr.SITE_GROUP_ID IN (900000001, 900000002) AND cr.PLUG_OUT > now() - INTERVAL '3 days';

-- events that could not be sent: waiting, sent again, given up
INSERT INTO EVENT_RETRY (ID, EVENT_ID, EVENT_TYPE, ATTRIBUTES, FAILURE, STATUS, RETRY_COUNT, CREATE_DATE,
                         LAST_RETRY_DATE) VALUES
    (900000001, 'demo-0001', 'SOCKET_STATUS_CHANGED', E'SOCKET_ID=900011022\nSTATUS=FAULTED\n',
     'Event queue refused the event', 'OPEN', 1, now() - INTERVAL '20 minutes', now() - INTERVAL '10 minutes'),
    (900000002, 'demo-0002', 'SUPPLY_STOPPED', E'SUPPLY_SESSION_ID=900000010\n', 'Event queue refused the event',
     'SENT', 2, now() - INTERVAL '3 days', now() - INTERVAL '3 days'),
    (900000003, 'demo-0003', 'BOOT_NOTIFICATION', E'PEDESTAL_ID=900000203\n', 'Listener timed out', 'SENT', 1,
     now() - INTERVAL '6 days', now() - INTERVAL '6 days'),
    (900000004, 'demo-0004', 'POWER_SECTION_MISCONFIGURED', E'POWER_SECTION_ID=900000006\n',
     'Mail server not reachable', 'FAILED', 5, now() - INTERVAL '2 days', now() - INTERVAL '1 day'),
    (900000005, 'demo-0005', 'QRPAY_ERROR', E'ID=900108036\nSTATUS=REMOTE_SUPPLY_START_FAILED\n',
     'Mail server not reachable', 'FAILED', 5, now() - INTERVAL '26 hours', now() - INTERVAL '20 hours'),
    (900000006, 'demo-0006', 'SOCKET_STATE_NOTIFICATION', E'SOCKET_ID=900013031\nSTATUS=FAULTED\n',
     'Event queue refused the event', 'OPEN', 0, now() - INTERVAL '5 minutes', NULL);

-- audit trail: logins, console actions, configuration, mails, commissioning
INSERT INTO AUDIT (ID, DATE_CREATED, CATEGORY, USERID, ACTION_DATE, ACTION_TYPE, ACTION_NAME, ACTION_GROUP_ID, STATUS,
                   ERROR_REASON, MANAGED_SERVER, INFO_FIELD_1, INFO_FIELD_2, MESSAGE, AUDIT_DATA)
SELECT 900000000 + a.N, now() - a.AGO, 'FUNCTIONAL', a.USERID, now() - a.AGO, a.TYPE, a.NAME, a.GRP,
       CASE WHEN a.ERR IS NULL THEN 'SUCCESS' ELSE 'ERROR' END, a.ERR, 'demo', a.INFO, NULL, a.MSG, a.DATA
  FROM (VALUES (1, INTERVAL '9 days', 'admin', 'LOGIN', 'login', '21', NULL, NULL, NULL, NULL),
               (2, INTERVAL '8 days', 'reception', 'LOGIN', 'login', '71', NULL, NULL, NULL, NULL),
               (3, INTERVAL '8 days', 'reception', 'LOGIN', 'login', '71', 'wrong password', NULL, NULL, NULL),
               (4, INTERVAL '7 days', 'reception', 'CONSOLE_ACTION', 'guest/checkOut', '71', NULL, NULL,
                '{"stayId":900000010}', NULL),
               (5, INTERVAL '6 days', 'admin', 'CONFIG_CHANGE', 'StaleSessionTimeoutInHours', NULL, NULL, NULL,
                '''24'' -> ''24''', NULL),
               (6, INTERVAL '5 days', 'service', 'CONSOLE_ACTION', 'pedestal/updateLocation', '21', NULL, NULL,
                '{"pedestalCode":"ARDECHE-P03"}', NULL),
               (7, INTERVAL '4 days', 'backoffice', 'CONSOLE_ACTION', 'consumptionRecord/reviewConsumptionRecord', '21',
                NULL, NULL, '{"confirmed":true,"comment":"Heat pump of a large caravan"}', NULL),
               (8, INTERVAL '3 days', 'host', 'CONSOLE_ACTION', 'tariff/applyTariffToSocket', '72',
                'HTTP 403: {"errorCode":"NOT_AUTHORIZED","message":"User ''host'' lacks right TARIFF_EDIT"}', NULL,
                '{"tariffId":900000002}', NULL),
               (9, INTERVAL '2 days', NULL, 'EMAIL', 'service.ardeche@campsite-power.example', NULL, NULL,
                'Pedestal ARDECHE-P03: socket FAULTED', NULL, 'Water in the socket housing of ARDECHE-P03_3'),
               (10, INTERVAL '26 hours', NULL, 'EMAIL', 'developer@campsite-power.example', NULL,
                'Mail server not reachable', 'QR payment error REMOTE_SUPPLY_START_FAILED', NULL, NULL),
               (11, INTERVAL '30 days', 'service', 'PEDESTAL_COMMISSIONING', 'GARDALAKE-P06', '21', NULL, NULL,
                'Commissioned at Garda Lakeside Camping', NULL),
               (12, INTERVAL '1 day', 'trainee', 'LOGIN', 'login', '71', 'user is INVITED', NULL, NULL, NULL),
               (13, INTERVAL '2 hours', 'admin', 'LOGIN', 'login', '21', NULL, NULL, NULL, NULL),
               (14, INTERVAL '90 minutes', 'admin', 'CONSOLE_ACTION', 'administration/setSystemProperty', '21', NULL,
                NULL, '{"key":"CheckoutSessionCloseDelayInMinutes","value":"0"}', NULL))
       AS a(N, AGO, USERID, TYPE, NAME, GRP, ERR, INFO, MSG, DATA);

-- configuration of the demo: the reviewers of suspect records, check-out closes the supplies at the next job run,
-- pedestals without a simulator are not reported offline for a week
UPDATE SYSTEM_PROPERTY SET PROPERTY_VALUE = 'records.review@campsite-power.example'
 WHERE PROPERTY_KEY = 'SuspectRecordEmail';
INSERT INTO SYSTEM_PROPERTY (PROPERTY_KEY, PROPERTY_VALUE, DESCRIPTION)
SELECT 'SuspectRecordEmail', 'records.review@campsite-power.example', 'Recipients of the mail about suspect records'
 WHERE NOT EXISTS (SELECT 1 FROM SYSTEM_PROPERTY WHERE PROPERTY_KEY = 'SuspectRecordEmail');
UPDATE SYSTEM_PROPERTY SET PROPERTY_VALUE = '0' WHERE PROPERTY_KEY = 'CheckoutSessionCloseDelayInMinutes';
-- the ERP exchange files of the demo go to a temporary directory
DELETE FROM SYSTEM_PROPERTY WHERE PROPERTY_KEY IN ('ErpOutboundDirectory', 'ErpInboundDirectory');
INSERT INTO SYSTEM_PROPERTY (PROPERTY_KEY, PROPERTY_VALUE, DESCRIPTION) VALUES
    ('ErpOutboundDirectory', '/tmp/campsite-power/erp/out', 'Directory of the files handed to the ERP (demo)'),
    ('ErpInboundDirectory', '/tmp/campsite-power/erp/in', 'Directory of the answer files of the ERP (demo)');
UPDATE SYSTEM_PROPERTY SET PROPERTY_VALUE = '10080' WHERE PROPERTY_KEY = 'PedestalHeartbeatTimeoutInMinutes';
