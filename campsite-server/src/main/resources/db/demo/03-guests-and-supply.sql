-- Demo data (profile demo): 44 days of guests, stays and supplies on every socket of the demo campsites.
-- Every pitch sees stays of 2-8 nights with free days in between; during a stay the guest's camping card supplies
-- in sessions of 1-3 nights, the socket reports its meter every 30 minutes. Free days bring a rental house card or
-- a QR payment, now and then a staff house card test. Two Istria pitches have seasonal residents. The pedestal
-- ARDECHE-P06 went silent 31 hours ago: its supplies stay open (stale). Random numbers come from a fixed seed, so
-- every seed looks the same relative to its day.

SELECT setseed(0.17);

DROP SEQUENCE IF EXISTS pg_temp.demo_session_seq;
DROP SEQUENCE IF EXISTS pg_temp.demo_log_seq;
DROP SEQUENCE IF EXISTS pg_temp.demo_guest_seq;
CREATE TEMPORARY SEQUENCE demo_session_seq START WITH 900000001;
CREATE TEMPORARY SEQUENCE demo_log_seq START WITH 900000001;
CREATE TEMPORARY SEQUENCE demo_guest_seq START WITH 900000001;

DROP TABLE IF EXISTS pg_temp.demo_stop;
CREATE TEMPORARY TABLE demo_stop
(
    SESSION_ID     BIGINT PRIMARY KEY,
    STOP_METER_KWH NUMERIC(19, 4) NOT NULL
);
DROP TABLE IF EXISTS pg_temp.demo_qr_session;
CREATE TEMPORARY TABLE demo_qr_session
(
    SESSION_ID BIGINT PRIMARY KEY
);

-- house cards of the campsites: staff cards for the pedestal tests, rental cards for guests without a camping card
INSERT INTO HOUSECARD (ID, GROUP_ID, HOUSE_CARD_TYPE, RFID, CARD_NUMBER, CREATION_DATE, LAST_USED, DELETED_DATE,
                       ACTIVE, DELETED)
SELECT 900000000 + c.NO * 10 + h.N, c.GROUP_ID, h.TYPE, '77' || c.NO || LPAD(h.N::TEXT, 6, '0'),
       c.PREFIX || '-' || h.TYPE || '-' || h.N, now() - INTERVAL '120 days', NULL, NULL, h.N <> 5, FALSE
  FROM (VALUES (1, 900000001, 'HRB'), (2, 900000002, 'ITG'), (3, 900000003, 'FRA')) AS c(NO, GROUP_ID, PREFIX),
       (VALUES (1, 'STAFF'), (2, 'STAFF'), (3, 'RENTAL'), (4, 'RENTAL'), (5, 'RENTAL')) AS h(N, TYPE);

-- one supply: the session, its meter readings, the socket's meter; closed supplies remember their stop meter
CREATE OR REPLACE FUNCTION pg_temp.demo_supply(p_socket BIGINT, p_card BIGINT, p_house BIGINT, p_stay BIGINT,
                                               p_plate TEXT, p_start TIMESTAMP, p_end TIMESTAMP, p_rate DOUBLE PRECISION,
                                               p_active BOOLEAN, p_stop_reason TEXT) RETURNS BIGINT
    LANGUAGE plpgsql AS
$f$
DECLARE
    v_id     BIGINT := nextval('pg_temp.demo_session_seq');
    v_rate   NUMERIC := p_rate::NUMERIC;
    v_meter  NUMERIC;
    v_max    INTEGER;
    v_t      TIMESTAMP;
    v_last   TIMESTAMP;
    v_f      NUMERIC;
    v_h      INTEGER;
BEGIN
    SELECT s.METER_KWH, CASE s.SOCKET_TYPE_ID WHEN 2 THEN 32 WHEN 3 THEN 10 ELSE 16 END
      INTO v_meter, v_max
      FROM SOCKET s
     WHERE s.ID = p_socket;
    INSERT INTO SUPPLY_SESSION (ID, SOCKET_ID, CARD_ID, HOUSECARD_ID, TRANSACTION_ID, START_METER_KWH, REMOTE_DATE,
                                CREATE_DATE, ACTIVE, MAX_CURRENT_A, STOP_REASON, STOP_DATE, STAY_ID, UNIT_PLATE)
    VALUES (v_id, p_socket, p_card, p_house, v_id::INTEGER, v_meter, p_start, p_start, p_active, v_max,
            CASE WHEN p_active THEN NULL ELSE p_stop_reason END, CASE WHEN p_active THEN NULL ELSE p_end END, p_stay,
            p_plate);
    INSERT INTO SOCKET_STATUS_LOG (ID, SOCKET_ID, CREATE_DATE, UPDATE_DATE, REMOTE_DATE, STATUS, METER_KWH,
                                   ERROR_FLAGS, CURRENT_A)
    VALUES (nextval('pg_temp.demo_log_seq'), p_socket, p_start, p_start, p_start, 'SUPPLYING', v_meter, 0, 0);
    v_last := p_start;
    v_t := p_start + INTERVAL '30 minutes';
    WHILE v_t < p_end LOOP
        v_h := EXTRACT(HOUR FROM v_t)::INTEGER;
        v_f := CASE WHEN v_h < 6 THEN 0.35 WHEN v_h BETWEEN 18 AND 22 THEN 1.4 ELSE 0.9 END;
        IF random() < 0.12 THEN
            v_f := 0;
        ELSE
            v_f := v_f * (0.4 + random()::NUMERIC);
        END IF;
        v_meter := v_meter + ROUND(v_rate * 0.5 * v_f, 3);
        INSERT INTO SOCKET_STATUS_LOG (ID, SOCKET_ID, CREATE_DATE, UPDATE_DATE, REMOTE_DATE, STATUS, METER_KWH,
                                       ERROR_FLAGS, CURRENT_A)
        VALUES (nextval('pg_temp.demo_log_seq'), p_socket, v_t, v_t, v_t, 'SUPPLYING', v_meter, 0,
                ROUND(v_rate * v_f * 1000 / 230, 2));
        v_last := v_t;
        v_t := v_t + INTERVAL '30 minutes';
    END LOOP;
    IF NOT p_active THEN
        v_meter := v_meter + ROUND(v_rate * 0.2 * random()::NUMERIC, 3);
        INSERT INTO SOCKET_STATUS_LOG (ID, SOCKET_ID, CREATE_DATE, UPDATE_DATE, REMOTE_DATE, STATUS, METER_KWH,
                                       ERROR_FLAGS, CURRENT_A)
        VALUES (nextval('pg_temp.demo_log_seq'), p_socket, p_end, p_end, p_end, 'AVAILABLE', v_meter, 0, 0);
        v_last := p_end;
        INSERT INTO pg_temp.demo_stop (SESSION_ID, STOP_METER_KWH) VALUES (v_id, v_meter);
    END IF;
    UPDATE SOCKET
       SET METER_KWH          = v_meter,
           MOD_DATE           = v_last,
           LAST_STATUS_UPDATE = v_last,
           STATUS             = CASE WHEN p_active THEN 'SUPPLYING' ELSE 'AVAILABLE' END
     WHERE ID = p_socket;
    RETURN v_id;
END
$f$;

-- a guest with unit, billing account (unless partner member or the odd guest without one), camping card and stay
CREATE OR REPLACE FUNCTION pg_temp.demo_guest(p_market TEXT, p_site BIGINT, p_pitch TEXT, p_arrival DATE,
                                              p_departure DATE, p_seasonal BOOLEAN, p_now TIMESTAMP,
                                              OUT o_guest BIGINT, OUT o_card BIGINT, OUT o_stay BIGINT,
                                              OUT o_plate TEXT)
    LANGUAGE plpgsql AS
$f$
DECLARE
    v_n        BIGINT := nextval('pg_temp.demo_guest_seq');
    v_i        INTEGER;
    v_first    TEXT[] := ARRAY ['Lukas', 'Anna', 'Jan', 'Sophie', 'Thomas', 'Emma', 'Marco', 'Giulia', 'Pierre',
        'Claire', 'Ivan', 'Petra', 'Martin', 'Eva', 'Tom', 'Lisa', 'Matteo', 'Sara', 'Hugo', 'Lea', 'Jonas', 'Mila'];
    v_last     TEXT[] := ARRAY ['Muller', 'Schmidt', 'de Vries', 'Janssen', 'Rossi', 'Bianchi', 'Dubois', 'Martin',
        'Horvat', 'Novak', 'Kowalski', 'Berger', 'Huber', 'Smit', 'Bakker', 'Ferrari', 'Moreau', 'Lefevre', 'Weber',
        'Fischer', 'Peeters', 'Kranjc', 'Hofer'];
    v_country  TEXT[] := ARRAY ['DE', 'AT', 'NL', 'IT', 'FR', 'HR', 'PL', 'SI', 'CH', 'BE', 'DK'];
    v_cities   TEXT[] := ARRAY ['Munich', 'Graz', 'Utrecht', 'Verona', 'Lyon', 'Zagreb', 'Krakow', 'Ljubljana',
        'Basel', 'Ghent', 'Aarhus'];
    v_group    BIGINT;
    v_member   BOOLEAN := FALSE;
    v_account  TEXT;
    v_iso      TEXT;
    v_unit     TEXT;
    v_checkout TIMESTAMP;
BEGIN
    v_i := (v_n - 900000000)::INTEGER;
    v_iso := v_country[1 + v_i % 11];
    IF p_market = 'HR' AND NOT p_seasonal AND v_i % 7 = 3 THEN
        v_member := TRUE;
        v_group := 900000011;
    ELSIF p_market = 'HR' THEN
        v_group := 31;
    ELSIF p_market = 'IT' THEN
        v_group := 900000012;
    ELSE
        v_group := 900000013;
    END IF;
    o_guest := v_n;
    o_card := v_n;
    o_plate := v_iso || '-' || CHR(65 + v_i % 26) || CHR(65 + (v_i / 26) % 26) || '-' || (100 + v_i % 900);
    INSERT INTO GUEST (ID, GROUP_ID, GUEST_TYPE, NAME, FIRST_NAME, EMAIL, STATUS, ERP_PARTNER_NO, CREATE_DATE, DELETED,
                       SALUTATION, CITY, COUNTRY_ISO, LANGUAGE, MOBILE, EMAIL_NOTIFICATION, PAYMENT_METHOD, CAR_PLATE,
                       MOD_DATE)
    VALUES (v_n, v_group, CASE WHEN p_seasonal THEN 'SEASONAL' ELSE 'TRANSIT' END, v_last[1 + (v_i * 7) % 23],
            v_first[1 + (v_i * 5) % 22],
            LOWER(v_first[1 + (v_i * 5) % 22]) || '.' || REPLACE(LOWER(v_last[1 + (v_i * 7) % 23]), ' ', '') || v_i
                || '@guests.example',
            'ACTIVE', CASE WHEN v_member THEN NULL ELSE '7' || LPAD(v_i::TEXT, 7, '0') END,
            p_arrival - 10, FALSE, CASE WHEN v_i % 2 = 0 THEN 'MR' ELSE 'MS' END, v_cities[1 + v_i % 11], v_iso,
            LOWER(CASE v_iso WHEN 'AT' THEN 'DE' WHEN 'CH' THEN 'DE' WHEN 'BE' THEN 'NL' ELSE v_iso END),
            '+49 151 ' || LPAD((v_i % 10000000)::TEXT, 7, '0'), v_i % 3 = 0,
            CASE WHEN p_seasonal THEN 'DIRECT_DEBIT' ELSE 'RECEPTION' END, o_plate, p_arrival);
    v_unit := CASE WHEN p_seasonal THEN 'CARAVAN' ELSE (ARRAY ['CAMPER', 'CARAVAN', 'TENT', 'CAMPER'])[1 + v_i % 4] END;
    INSERT INTO UNIT (ID, GUEST_ID, UNIT_TYPE, PLATE, LENGTH_M, DESCRIPTION, CREATE_DATE, DELETED)
    VALUES (v_n, v_n, v_unit, CASE WHEN v_unit = 'TENT' THEN NULL ELSE o_plate END,
            CASE v_unit WHEN 'TENT' THEN 4.0 WHEN 'CAMPER' THEN 6.5 + (v_i % 3) ELSE 7.5 END,
            INITCAP(LOWER(v_unit)), p_arrival - 10, FALSE);
    IF NOT v_member AND NOT (p_market = 'IT' AND v_i % 23 = 0) THEN
        v_account := '40' || LPAD(v_i::TEXT, 8, '0');
        INSERT INTO BILLING_ACCOUNT (ID, GUEST_ID, ERP_ACCOUNT_NO, DESCRIPTION, CREATE_DATE, CLOSED, CLOSE_DATE)
        VALUES (v_n, v_n, v_account, CASE WHEN p_seasonal THEN 'Seasonal pitch' ELSE 'Power of the stays' END,
                p_arrival - 10, FALSE, NULL);
    END IF;
    INSERT INTO CAMPING_CARD (ID, CARD_RFID, CARD_NUMBER, STATUS, GUEST_ID, VALID_FROM, VALID_TO, BILLING_ACCOUNT_NO,
                              DELETED, CREATE_DATE, UPLOAD_DATE, PARTNER_SYNC_STATE, TARGET_PARTNER_SYNC_STATE)
    VALUES (v_n, '4' || LPAD(v_i::TEXT, 9, '0'),
            CASE WHEN v_member THEN 'ACC-' ELSE p_market || '-' END || (100000 + v_i),
            'ACTIVATED', v_n, p_arrival, p_departure, v_account, FALSE, p_arrival - 10,
            CASE WHEN v_member THEN p_arrival - 10 END,
            CASE WHEN v_member THEN 'NEVER' WHEN v_i % 5 = 0 THEN 'MODIFY' WHEN v_i % 5 = 1 THEN 'NOT' ELSE 'ADD' END,
            CASE WHEN v_member THEN 'NEVER' END);
    IF p_seasonal THEN
        o_stay := NULL;
        RETURN;
    END IF;
    v_checkout := CASE WHEN p_departure + TIME '10:15' < p_now THEN p_departure + TIME '09:30' + (v_i % 60) * INTERVAL '1 minute' END;
    o_stay := v_n;
    INSERT INTO STAY (ID, GUEST_ID, SITE_GROUP_ID, PITCH_NO, CARD_ID, UNIT_ID, ARRIVAL_DATE, DEPARTURE_DATE, ADULTS,
                      CHILDREN, CHECK_IN_DATE, CHECKED_IN_BY, CHECK_OUT_DATE, CHECKED_OUT_BY, REMARKS, DELETED)
    VALUES (v_n, v_n, p_site, p_pitch, v_n, v_n, p_arrival, p_departure, 1 + v_i % 3, (v_i / 3) % 3,
            p_arrival + TIME '14:00' + (v_i % 90) * INTERVAL '1 minute',
            CASE p_market WHEN 'HR' THEN 'reception' WHEN 'IT' THEN 'host' ELSE 'admin.fr' END,
            v_checkout, CASE WHEN v_checkout IS NOT NULL THEN
                CASE p_market WHEN 'HR' THEN 'reception' WHEN 'IT' THEN 'host' ELSE 'admin.fr' END END,
            CASE WHEN v_i % 11 = 0 THEN 'Late arrival, key at the gate' END, FALSE);
END
$f$;

DO
$$
    DECLARE
        s          RECORD;
        v_now      TIMESTAMP;
        v_day      DATE;
        v_nights   INTEGER;
        v_arr      DATE;
        v_dep      DATE;
        v_guest    RECORD;
        v_t        TIMESTAMP;
        v_end      TIMESTAMP;
        v_limit    TIMESTAMP;
        v_rate     NUMERIC;
        v_active   BOOLEAN;
        v_session  BIGINT;
        v_reason   TEXT;
        v_seasonal BOOLEAN;
        v_house    BIGINT;
        v_reserved BOOLEAN;
    BEGIN
        FOR s IN
            SELECT so.ID, so.PITCH_NO, p.ID AS PEDESTAL_ID, p.GROUP_ID AS SITE_ID,
                   CASE p.GROUP_ID WHEN 900000001 THEN 'HR' WHEN 900000002 THEN 'IT' ELSE 'FR' END AS MARKET,
                   (p.GROUP_ID - 900000000) AS SITE_NO
              FROM SOCKET so
              JOIN PEDESTAL p ON p.ID = so.PEDESTAL_ID
             WHERE so.ID >= 900000000
             ORDER BY so.ID
        LOOP
            -- sockets kept free at the end for the faults and QR payments of 04-operations.sql
            v_reserved := s.ID IN (900011024, 900012054, 900013034, 900012041, 900013021);
            v_now := CASE WHEN s.PEDESTAL_ID = 900000306 THEN now() - INTERVAL '31 hours'
                          WHEN s.ID = 900011024 THEN now() - INTERVAL '6 hours'
                          WHEN s.ID = 900012054 THEN now() - INTERVAL '2 hours'
                          WHEN s.ID = 900013034 THEN now() - INTERVAL '50 hours'
                          WHEN s.ID = 900012041 THEN now() - INTERVAL '3 hours'
                          WHEN s.ID = 900013021 THEN now() - INTERVAL '5 hours'
                          ELSE now() - INTERVAL '4 minutes' END;
            v_seasonal := s.SITE_ID = 900000001 AND s.PITCH_NO IN ('B09', 'B10');
            IF v_seasonal THEN
                -- seasonal resident: on the pitch since spring, plugged in for a week at a time
                SELECT * INTO v_guest
                  FROM pg_temp.demo_guest(s.MARKET, s.SITE_ID, s.PITCH_NO,
                                          (date_trunc('year', v_now) + INTERVAL '3 months')::DATE,
                                          (date_trunc('year', v_now) + INTERVAL '10 months' - INTERVAL '1 day')::DATE,
                                          TRUE, v_now);
                v_rate := 0.55 + random() * 0.3;
                v_t := date_trunc('day', v_now) - INTERVAL '44 days' + TIME '08:00';
                LOOP
                    v_end := v_t + INTERVAL '7 days' - (random() * 6) * INTERVAL '1 hour';
                    v_active := v_end > v_now;
                    v_session := pg_temp.demo_supply(s.ID, v_guest.o_card, NULL, NULL, v_guest.o_plate, v_t,
                                                     LEAST(v_end, v_now), v_rate, v_active, 'LOCAL');
                    EXIT WHEN v_active;
                    v_t := v_end + (1 + random() * 3) * INTERVAL '1 hour';
                    EXIT WHEN v_t >= v_now;
                END LOOP;
                CONTINUE;
            END IF;

            v_day := (date_trunc('day', v_now) - INTERVAL '44 days')::DATE;
            LOOP
                IF random() < 0.6 THEN
                    -- free days: on the first a guest without a card, a QR payment or a pedestal test by the staff
                    v_t := v_day + TIME '10:00' + (random() * 3) * INTERVAL '1 hour';
                    IF v_t < v_now - INTERVAL '6 hours' THEN
                        IF random() < 0.45 AND s.MARKET <> 'FR' THEN
                            v_house := 900000000 + s.SITE_NO * 10 + 3 + (s.ID % 2);
                            v_session := pg_temp.demo_supply(s.ID, NULL, v_house, NULL, NULL, v_t,
                                                             v_t + (4 + random() * 5) * INTERVAL '1 hour',
                                                             0.6 + random() * 0.6, FALSE, 'LOCAL');
                        ELSIF random() < 0.6 THEN
                            v_session := pg_temp.demo_supply(s.ID, NULL, NULL, NULL, NULL, v_t,
                                                             v_t + (2 + random() * 3) * INTERVAL '1 hour',
                                                             0.8 + random() * 0.8, FALSE, 'REMOTE');
                            INSERT INTO pg_temp.demo_qr_session (SESSION_ID) VALUES (v_session);
                        ELSE
                            v_house := 900000000 + s.SITE_NO * 10 + 1 + (s.ID % 2);
                            v_session := pg_temp.demo_supply(s.ID, NULL, v_house, NULL, NULL, v_t,
                                                             v_t + (20 + random() * 30) * INTERVAL '1 minute',
                                                             1.2, FALSE, 'LOCAL');
                        END IF;
                    END IF;
                    v_day := v_day + 1 + FLOOR(random() * 4)::INTEGER;
                END IF;
                v_nights := 2 + FLOOR(random() * 7)::INTEGER;
                v_arr := v_day;
                v_dep := v_day + v_nights;
                EXIT WHEN v_arr + TIME '14:00' > v_now;
                SELECT * INTO v_guest
                  FROM pg_temp.demo_guest(s.MARKET, s.SITE_ID, s.PITCH_NO, v_arr, v_dep, FALSE, v_now);
                v_rate := 0.15 + random() * 0.45;
                v_t := v_arr + TIME '14:30' + (random() * 3) * INTERVAL '1 hour';
                v_limit := v_dep + TIME '09:00' + (random() * 60) * INTERVAL '1 minute';
                LOOP
                    EXIT WHEN v_t >= v_now OR v_t >= v_limit - INTERVAL '2 hours';
                    v_end := (v_t::DATE + 1 + FLOOR(random() * 3)::INTEGER) + TIME '08:30'
                        + (random() * 150) * INTERVAL '1 minute';
                    v_reason := 'LOCAL';
                    IF v_end >= v_limit - INTERVAL '3 hours' THEN
                        v_end := v_limit;
                        v_reason := CASE WHEN random() < 0.3 THEN 'CHECKOUT' ELSE 'UNPLUGGED' END;
                    ELSIF random() < 0.04 THEN
                        v_reason := 'BREAKER_TRIP';
                    ELSIF random() < 0.03 THEN
                        v_reason := 'POWER_LOSS';
                    END IF;
                    IF v_end > v_now AND v_t < v_now - INTERVAL '2 hours' AND random() < 0.3 THEN
                        -- unplugged a while ago, out for the day
                        v_end := v_now - (10 + random() * 90) * INTERVAL '1 minute';
                        v_reason := 'UNPLUGGED';
                    END IF;
                    v_active := v_end > v_now AND NOT v_reserved;
                    v_session := pg_temp.demo_supply(s.ID, v_guest.o_card, NULL, v_guest.o_stay, v_guest.o_plate,
                                                     v_t, LEAST(v_end, v_now), v_rate, v_active, v_reason);
                    EXIT WHEN v_active OR v_end > v_now;
                    -- out for the day, or a night without power
                    v_t := v_end + CASE WHEN random() < 0.35 THEN 8 + random() * 16 ELSE 1 + random() * 5 END
                        * INTERVAL '1 hour';
                END LOOP;
                v_day := v_dep;
            END LOOP;
        END LOOP;
    END
$$;

-- ARDECHE-P06 went silent while guests were plugged in. The stays above are random and depend on the time of the
-- seed, so when none of its sockets is still supplying, its last card supply stays open: the stale sessions of the
-- silent pedestal are there at every time of day.
DO
$$
    DECLARE
        v_session BIGINT;
    BEGIN
        IF NOT EXISTS (SELECT 1
                         FROM SUPPLY_SESSION ss
                         JOIN SOCKET so ON so.ID = ss.SOCKET_ID
                        WHERE so.PEDESTAL_ID = 900000306 AND ss.ACTIVE) THEN
            SELECT ss.ID INTO v_session
              FROM SUPPLY_SESSION ss
              JOIN SOCKET so ON so.ID = ss.SOCKET_ID
              LEFT JOIN STAY st ON st.ID = ss.STAY_ID
             WHERE so.PEDESTAL_ID = 900000306 AND ss.CARD_ID IS NOT NULL
             ORDER BY (st.ID IS NOT NULL AND st.CHECK_OUT_DATE IS NULL) DESC, ss.STOP_DATE DESC
             LIMIT 1;
            DELETE FROM SOCKET_STATUS_LOG l
             USING SUPPLY_SESSION ss
             WHERE ss.ID = v_session AND l.SOCKET_ID = ss.SOCKET_ID AND l.CREATE_DATE = ss.STOP_DATE
               AND l.STATUS = 'AVAILABLE';
            DELETE FROM pg_temp.demo_stop WHERE SESSION_ID = v_session;
            UPDATE SOCKET so
               SET STATUS = 'SUPPLYING'
              FROM SUPPLY_SESSION ss
             WHERE ss.ID = v_session AND so.ID = ss.SOCKET_ID;
            UPDATE SUPPLY_SESSION SET ACTIVE = TRUE, STOP_REASON = NULL, STOP_DATE = NULL WHERE ID = v_session;
        END IF;
    END
$$;

-- consumption records of the closed supplies, priced with the tariff of the socket, pedestal or market
INSERT INTO CONSUMPTION_RECORD (ID, RECORD_NO, SUPPLY_SESSION_ID, TRANSACTION_ID, SOCKET_ID, SOCKET_CODE, SOCKET_NAME,
                                PITCH_NO, PEDESTAL_ID, PEDESTAL_CODE, PEDESTAL_REFERENCE_ID, PEDESTAL_TYPE,
                                SITE_GROUP_ID, SITE_TENANT, CAMPSITE_CODE, CAMPSITE_NAME, CITY, ZIPCODE, STREET,
                                HOUSE_NUMBER, COUNTRY_ISO, CARD_ID, CARD_RFID, CARD_NUMBER, HOUSECARD_ID,
                                HOUSE_CARD_TYPE, GUEST_ID, GUEST_NAME, BILLING_ACCOUNT_NO, ERP_PARTNER_NO,
                                ISSUER_GROUP_ID, ISSUER_TENANT, STAY_ID, UNIT_PLATE, PAYMENT_CHANNEL, BILLABLE,
                                SETTLED_BY_CAMPSITE, HOUSE_CARD_RENTAL, START_METER_KWH, STOP_METER_KWH, KWH, PLUG_IN,
                                PLUG_OUT, DURATION_S, NIGHTS, MAX_CURRENT_A, STOP_REASON, COST, CURRENCY,
                                PRICING_STATUS, PRICING_FAILURE_REASON, TARIFF_ID, TARIFF_NAME, VALIDATION_STATE,
                                SUSPECT_REASON, REVIEWED_BY, REVIEW_DATE, REVIEW_COMMENT, EXPORT_STATUS, CREATE_DATE)
SELECT x.ID, x.CAMPSITE_CODE || '-' || TO_CHAR(x.PLUG_OUT, 'YYYYMMDD') || '-' || LPAD((x.ID - 900000000)::TEXT, 6, '0'),
       x.ID, x.TRANSACTION_ID, x.SOCKET_ID, x.SOCKET_CODE, x.SOCKET_NAME, x.PITCH_NO, x.PEDESTAL_ID, x.PEDESTAL_CODE,
       x.REFERENCE_ID, x.PEDESTAL_TYPE, x.SITE_GROUP_ID, x.SITE_TENANT, x.CAMPSITE_CODE, x.CAMPSITE_NAME, x.CITY,
       x.ZIPCODE, x.STREET, x.HOUSE_NUMBER, x.COUNTRY_ISO, x.CARD_ID, x.CARD_RFID, x.CARD_NUMBER, x.HOUSECARD_ID,
       x.HOUSE_CARD_TYPE, x.GUEST_ID, x.GUEST_NAME, x.BILLING_ACCOUNT_NO, x.ERP_PARTNER_NO, x.ISSUER_GROUP_ID,
       x.ISSUER_TENANT, x.STAY_ID, x.UNIT_PLATE, x.PAYMENT_CHANNEL, x.BILLABLE, x.SETTLED_BY_CAMPSITE,
       x.HOUSE_CARD_RENTAL, x.START_METER_KWH, x.STOP_METER_KWH, x.KWH, x.PLUG_IN, x.PLUG_OUT,
       EXTRACT(EPOCH FROM (x.PLUG_OUT - x.PLUG_IN))::BIGINT, x.NIGHTS, x.MAX_CURRENT_A, x.STOP_REASON,
       CASE WHEN NOT x.BILLABLE THEN NULL
            ELSE ROUND(CASE x.TARIFF_ID
                           WHEN 1 THEN x.KWH * 0.45
                           WHEN 2 THEN x.KWH * 0.55
                           WHEN 3 THEN x.KWH * 0.50
                           WHEN 4 THEN x.KWH * 0.38
                           WHEN 900000001 THEN x.KWH * 0.42 + 0.50
                           WHEN 900000002 THEN x.KWH * 0.55
                           ELSE GREATEST(x.NIGHTS, 1) * 3.50
                               + x.KWH * CASE WHEN x.KWH <= 15 THEN 0.25 ELSE 0.45 END
                       END, 2) END,
       CASE WHEN x.BILLABLE THEN 'EUR' END,
       CASE WHEN x.BILLABLE THEN 'PRICED' ELSE 'NOT_BILLABLE' END, NULL,
       CASE WHEN x.BILLABLE THEN x.TARIFF_ID END, CASE WHEN x.BILLABLE THEN t.NAME END,
       'VALID', NULL, NULL, NULL, NULL, 'NEW', x.PLUG_OUT + INTERVAL '20 seconds'
  FROM (SELECT ss.ID, ss.TRANSACTION_ID, so.ID AS SOCKET_ID, so.SOCKET_CODE, so.NAME AS SOCKET_NAME, so.PITCH_NO,
               p.ID AS PEDESTAL_ID, p.CODE AS PEDESTAL_CODE, p.REFERENCE_ID, pt.NAME AS PEDESTAL_TYPE,
               p.GROUP_ID AS SITE_GROUP_ID, st.NAME AS SITE_TENANT, l.CAMPSITE_CODE, g.DISPLAYNAME AS CAMPSITE_NAME,
               l.CITY, l.ZIPCODE, l.STREET, l.HOUSE_NUMBER, l.COUNTRY_ISO, cc.ID AS CARD_ID, cc.CARD_RFID,
               cc.CARD_NUMBER, hc.ID AS HOUSECARD_ID, hc.HOUSE_CARD_TYPE, gu.ID AS GUEST_ID,
               TRIM(COALESCE(gu.FIRST_NAME || ' ', '') || gu.NAME) AS GUEST_NAME, cc.BILLING_ACCOUNT_NO,
               gu.ERP_PARTNER_NO, gu.GROUP_ID AS ISSUER_GROUP_ID,
               COALESCE(it.NAME, CASE WHEN gu.GROUP_ID = 31 THEN 'Campsite network Croatia' END) AS ISSUER_TENANT,
               ss.STAY_ID, ss.UNIT_PLATE,
               CASE WHEN ss.CARD_ID IS NOT NULL THEN 'CARD' WHEN ss.HOUSECARD_ID IS NOT NULL THEN 'HOUSE_CARD'
                    ELSE 'QR_APP' END AS PAYMENT_CHANNEL,
               COALESCE(hc.HOUSE_CARD_TYPE, '') <> 'STAFF' AS BILLABLE,
               COALESCE(hc.HOUSE_CARD_TYPE = 'RENTAL', FALSE) AS SETTLED_BY_CAMPSITE,
               COALESCE(hc.HOUSE_CARD_TYPE = 'RENTAL', FALSE) AS HOUSE_CARD_RENTAL,
               ss.START_METER_KWH, ds.STOP_METER_KWH, ds.STOP_METER_KWH - ss.START_METER_KWH AS KWH,
               ss.CREATE_DATE AS PLUG_IN, ss.STOP_DATE AS PLUG_OUT,
               (ss.STOP_DATE::DATE - ss.CREATE_DATE::DATE) AS NIGHTS, ss.MAX_CURRENT_A, ss.STOP_REASON,
               CASE WHEN gu.GUEST_TYPE = 'SEASONAL' THEN 4
                    ELSE COALESCE(tas.TARIFF_ID, tap.TARIFF_ID,
                                  CASE p.GROUP_ID WHEN 900000001 THEN 1 WHEN 900000002 THEN 2 ELSE 3 END) END
                   AS TARIFF_ID
          FROM SUPPLY_SESSION ss
          JOIN pg_temp.demo_stop ds ON ds.SESSION_ID = ss.ID
          JOIN SOCKET so ON so.ID = ss.SOCKET_ID
          JOIN PEDESTAL p ON p.ID = so.PEDESTAL_ID
          JOIN PEDESTAL_TYPE pt ON pt.ID = p.PEDESTAL_TYPE_ID
          JOIN LOCATION l ON l.ID = p.LOCATION_ID
          JOIN TBL_GROUP g ON g.ID = p.GROUP_ID
          LEFT JOIN GRP_ATTR_SITE_TENANT st ON st.GROUP_ID = p.GROUP_ID
          LEFT JOIN CAMPING_CARD cc ON cc.ID = ss.CARD_ID
          LEFT JOIN GUEST gu ON gu.ID = cc.GUEST_ID
          LEFT JOIN GRP_ATTR_ISSUER_TENANT it ON it.GROUP_ID = gu.GROUP_ID
          LEFT JOIN HOUSECARD hc ON hc.ID = ss.HOUSECARD_ID
          LEFT JOIN TARIFF_APPLICABILITY tas ON tas.SOCKET_ID = so.ID
          LEFT JOIN TARIFF_APPLICABILITY tap ON tap.PEDESTAL_ID = p.ID) x
  JOIN TARIFF t ON t.ID = x.TARIFF_ID;

UPDATE HOUSECARD hc
   SET LAST_USED = (SELECT MAX(ss.CREATE_DATE) FROM SUPPLY_SESSION ss WHERE ss.HOUSECARD_ID = hc.ID)
 WHERE hc.ID >= 900000000;

-- Welcome card pools: per market a pool guest keeps the cards the reception hands to guests who arrive without a card
INSERT INTO GUEST (ID, GROUP_ID, GUEST_TYPE, NAME, FIRST_NAME, STATUS, CREATE_DATE, DELETED, MOD_DATE)
SELECT 900090000 + m.NO, m.POOL_GROUP_ID, 'TRANSIT', 'Welcome card pool', m.MARKET, 'ACTIVE',
       CURRENT_DATE - 60, FALSE, CURRENT_DATE - 60
  FROM (VALUES (1, 'HR', 41), (2, 'IT', 42), (3, 'FR', 43)) AS m(NO, MARKET, POOL_GROUP_ID);

INSERT INTO CAMPING_CARD (ID, CARD_RFID, CARD_NUMBER, STATUS, GUEST_ID, VALID_FROM, VALID_TO, BILLING_ACCOUNT_NO,
                          DELETED, CREATE_DATE, PARTNER_SYNC_STATE)
SELECT 900090100 + m.NO * 20 + n, '8' || LPAD((m.NO * 100 + n)::TEXT, 9, '0'), m.MARKET || '-W' || LPAD(n::TEXT, 3, '0'),
       'ACTIVATED', 900090000 + m.NO, NULL, NULL, '7700' || LPAD((m.NO * 100 + n)::TEXT, 8, '0'), FALSE,
       CURRENT_DATE - 60, 'NOT'
  FROM (VALUES (1, 'HR'), (2, 'IT'), (3, 'FR')) AS m(NO, MARKET), generate_series(1, 8) AS n;
