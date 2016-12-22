-- Demo data (profile demo): campsites, staff, pedestals and sockets, power sections, map, tariffs, issuer partner.
-- Three network campsites (Istria coast, Lake Garda, Ardeche), each 240 m x 125 m with sections A (west) and B
-- (east); a section has 3 rows of 4 pitches, every row one pedestal with 4 sockets, socket n supplying pitch n of
-- the row. Ids from 900000000 on: pedestal 9000001pp/9000002pp/9000003pp, its sockets 9000ccpp0n.

INSERT INTO TBL_GROUP (ID, PARENT_ID, NAME, TYPENAME, DISPLAYNAME) VALUES
    (900000001, 11, 'ISTRIA BAY CAMP', 'SITE', 'Camp Istria Bay'),
    (900000002, 12, 'GARDA LAKESIDE', 'SITE', 'Garda Lakeside Camping'),
    (900000003, 13, 'ARDECHE RIVER CAMP', 'SITE', 'Ardeche River Camp'),
    (900000011, 1, 'ADRIA CAMPING CLUB', 'ISSUER', 'Adria Camping Club'),
    (900000012, 2, 'IT NETWORK GUESTS', 'ISSUER', 'Network guests Italy'),
    (900000013, 3, 'FR NETWORK GUESTS', 'ISSUER', 'Network guests France');

INSERT INTO GRP_ATTR_SITE_TENANT (ID, GROUP_ID, NAME) VALUES
    (900000001, 900000001, 'Camp Istria Bay d.o.o.'),
    (900000002, 900000002, 'Garda Lakeside S.r.l.'),
    (900000003, 900000003, 'Ardeche River Camp SARL');
INSERT INTO GRP_ATTR_ISSUER_TENANT (ID, GROUP_ID, NAME) VALUES
    (900000011, 900000011, 'Adria Camping Club'),
    (900000012, 900000012, 'Campsite network Italy'),
    (900000013, 900000013, 'Campsite network France');
INSERT INTO GRP_ATTR_NOTIFICATION (ID, GROUP_ID, NAME, EMAIL, NOTIFICATION_TYPE) VALUES
    (900000021, 900000001, 'Pedestal service Istria', 'service.istria@campsite-power.example', 'PEDESTAL_STATUS'),
    (900000022, 900000002, 'Pedestal service Garda', 'service.garda@campsite-power.example', 'PEDESTAL_STATUS'),
    (900000023, 900000003, 'Pedestal service Ardeche', 'service.ardeche@campsite-power.example', 'PEDESTAL_STATUS'),
    (900000024, 900000001, 'Firmware Istria', 'service.istria@campsite-power.example', 'FIRMWARE_STATUS');
INSERT INTO GRP_ATTR_COMMLOG (ID, GROUP_ID, NAME, ACTIVE) VALUES
    (900000031, 900000001, 'Communication log Istria', TRUE),
    (900000032, 900000002, 'Communication log Garda', TRUE),
    (900000033, 900000003, 'Communication log Ardeche', FALSE);
INSERT INTO GRP_ATTR_LOCAL_LIST (ID, GROUP_ID, LOCAL_LIST_ALLOWED, INCLUDE_CARDS, INCLUDE_HOUSE_CARDS) VALUES
    (900000041, 900000001, TRUE, TRUE, TRUE),
    (900000042, 900000002, TRUE, FALSE, TRUE);
INSERT INTO GRP_ATTR_EXPORT_CAMPSITE_NAME (ID, GROUP_ID, NAME) VALUES
    (900000051, 900000001, 'ISTRIA BAY');

-- acceptance agreements: network cards of Croatia at Garda and Ardeche, the club's cards at Istria Bay, the Italian
-- network guests at Istria Bay
INSERT INTO GROUP_ASSOCIATEDGROUP (GROUP_ID, ASSOCIATEDGROUP_ID) VALUES
    (31, 900000002),
    (31, 900000003),
    (900000011, 900000001),
    (900000012, 900000001);

-- console users (the seeder sets the passwords: demo-<username>); trainee is invited and has no password yet
INSERT INTO USERINFO (ID, USERNAME, PASSWORD_HASH, NAME, EMAIL, STATUS, EMAIL_LOCALE, GROUP_ID, CREATE_DATE, DELETED)
VALUES
    (900000001, 'reception', NULL, 'Ana Reception', 'reception@campsite-power.example', 'ACTIVE', 'hr', 71,
     now() - INTERVAL '200 days', FALSE),
    (900000002, 'host', NULL, 'Marco Host', 'host@campsite-power.example', 'ACTIVE', 'it', 72,
     now() - INTERVAL '180 days', FALSE),
    (900000003, 'service', NULL, 'Ivan Service', 'service@campsite-power.example', 'ACTIVE', 'hr', 21,
     now() - INTERVAL '300 days', FALSE),
    (900000004, 'backoffice', NULL, 'Petra Back Office', 'backoffice@campsite-power.example', 'ACTIVE', 'hr', 21,
     now() - INTERVAL '250 days', FALSE),
    (900000005, 'admin', NULL, 'Network Admin', 'admin@campsite-power.example', 'ACTIVE', 'en', 21,
     now() - INTERVAL '400 days', FALSE),
    (900000006, 'admin.it', NULL, 'Giulia Admin', 'admin.it@campsite-power.example', 'ACTIVE', 'it', 22,
     now() - INTERVAL '400 days', FALSE),
    (900000007, 'admin.fr', NULL, 'Claire Admin', 'admin.fr@campsite-power.example', 'ACTIVE', 'fr', 23,
     now() - INTERVAL '400 days', FALSE),
    (900000008, 'qrpay-app', NULL, 'Payment app', 'qrpay-app@campsite-power.example', 'ACTIVE', 'en', 21,
     now() - INTERVAL '120 days', FALSE),
    (900000009, 'trainee', NULL, 'Luka Trainee', 'trainee@campsite-power.example', 'INVITED', 'hr', 71,
     now() - INTERVAL '2 days', FALSE);

INSERT INTO USERINFO_USERROLE (USERINFO_ID, USERROLE_ID)
SELECT u.ID, r.ID
  FROM (VALUES (900000001, 'RECEPTION'), (900000002, 'CAMPSITE_MANAGER'), (900000003, 'SERVICE_TECHNICIAN'),
               (900000004, 'BACK_OFFICE'), (900000005, 'MARKET_ADMIN'), (900000006, 'MARKET_ADMIN'),
               (900000007, 'MARKET_ADMIN'), (900000008, 'QRPAY_APP_CLIENT'), (900000009, 'RECEPTION'))
       AS u(ID, ROLE_NAME)
  JOIN USERROLE r ON r.NAME = u.ROLE_NAME;

INSERT INTO USERINFO_GROUP (USERINFO_ID, GROUP_ID) VALUES
    (900000001, 71), (900000001, 900000001), (900000001, 1),
    (900000002, 72), (900000002, 900000002), (900000002, 2),
    (900000003, 21), (900000003, 11),
    (900000004, 21), (900000004, 11), (900000004, 1),
    (900000005, 21), (900000005, 11), (900000005, 1),
    (900000006, 22), (900000006, 12), (900000006, 2),
    (900000007, 23), (900000007, 13), (900000007, 3),
    (900000008, 21),
    (900000009, 71), (900000009, 900000001), (900000009, 1);

INSERT INTO PEDESTAL_TYPE (ID, NAME, MANUFACTURER, LINK_VERSION, REMOTE_BREAKER_RESET, LOCAL_CARD_LIST,
                           LOCAL_CARD_LIST_CAPACITY, MAX_SOCKETS, DESCRIPTION, CODE) VALUES
    (900000001, 'Marina M4', 'NORDPOL ELECTRIC', 'V1', FALSE, TRUE, 500, 4, 'Four sockets, local card list', 41),
    (900000002, 'AQ-400', 'ADRIATEC', 'V2', TRUE, TRUE, 1000, 4, 'Four sockets, remote breaker reset', 42),
    (900000003, 'Campus 3', 'VOLTERRA', 'V3', TRUE, FALSE, NULL, 6, 'Up to six sockets, vendor status data', 43);

INSERT INTO POWER_SECTION (ID, SECTION_NAME, MAX_CURRENT_A, ALGORITHM_BEAN_NAME) VALUES
    (900000001, 'ISTRIA BAY SECTION A', 80, 'nominalCurrentSum'),
    (900000002, 'ISTRIA BAY SECTION B', 63, 'nominalCurrentSum'),
    (900000003, 'GARDA LAKESIDE SECTION A', 80, 'nominalCurrentSum'),
    (900000004, 'GARDA LAKESIDE SECTION B', 50, 'nominalCurrentSum'),
    (900000005, 'ARDECHE RIVER SECTION A', 63, 'nominalCurrentSum'),
    (900000006, 'ARDECHE RIVER SECTION B', 40, NULL);

-- GeoJSON polygon (WGS 84) of a box given in metres east (x) and north (y) of the campsite's south-west corner
CREATE OR REPLACE FUNCTION pg_temp.demo_rect(lat0 NUMERIC, lon0 NUMERIC, x0 NUMERIC, y0 NUMERIC, x1 NUMERIC,
                                             y1 NUMERIC) RETURNS TEXT
    LANGUAGE SQL IMMUTABLE AS
$$
SELECT json_build_object('type', 'Polygon', 'coordinates', json_build_array(json_build_array(
        json_build_array(ROUND(lon0 + x0 / (111320 * COS(RADIANS(lat0))::NUMERIC), 7), ROUND(lat0 + y0 / 111320, 7)),
        json_build_array(ROUND(lon0 + x1 / (111320 * COS(RADIANS(lat0))::NUMERIC), 7), ROUND(lat0 + y0 / 111320, 7)),
        json_build_array(ROUND(lon0 + x1 / (111320 * COS(RADIANS(lat0))::NUMERIC), 7), ROUND(lat0 + y1 / 111320, 7)),
        json_build_array(ROUND(lon0 + x0 / (111320 * COS(RADIANS(lat0))::NUMERIC), 7), ROUND(lat0 + y1 / 111320, 7)),
        json_build_array(ROUND(lon0 + x0 / (111320 * COS(RADIANS(lat0))::NUMERIC), 7), ROUND(lat0 + y0 / 111320, 7))
    )))::TEXT
$$;

DO
$$
    DECLARE
        c         RECORD;
        v_section INTEGER;
        v_row     INTEGER;
        v_col     INTEGER;
        v_ped_no  INTEGER;
        v_ped_id  BIGINT;
        v_loc_id  BIGINT;
        v_type    BIGINT;
        v_code    TEXT;
        v_sec_id  BIGINT;
        v_pitch   TEXT;
        v_x       NUMERIC;
        v_y       NUMERIC;
        v_area_id BIGINT := 900000000;
        v_sock_id BIGINT;
    BEGIN
        FOR c IN
            SELECT *
              FROM (VALUES (1, 900000001::BIGINT, 'ISTRIABAY', 'Camp Istria Bay', 45.1497::NUMERIC, 13.6011::NUMERIC,
                            'Vrsar', '52450', 'Petalon', '1', 'Istria', 'HR', 900000001::BIGINT),
                           (2, 900000002::BIGINT, 'GARDALAKE', 'Garda Lakeside Camping', 45.5003, 10.7319,
                            'Lazise', '37017', 'Via Gardesana', '42', 'Veneto', 'IT', 900000003::BIGINT),
                           (3, 900000003::BIGINT, 'ARDECHE', 'Ardeche River Camp', 44.3986, 4.3889,
                            'Vallon-Pont-d''Arc', '07150', 'Route des Gorges', '7', 'Auvergne-Rhone-Alpes', 'FR',
                            900000005::BIGINT))
                       AS t(NO, GROUP_ID, CODE, NAME, LAT, LON, CITY, ZIP, STREET, HOUSE, REGION, COUNTRY, SECTION_ID)
             ORDER BY 1
        LOOP
            v_area_id := v_area_id + 1;
            INSERT INTO CAMPSITE_MAP_AREA (ID, SITE_GROUP_ID, AREA_TYPE, NAME, PITCH_NO, POWER_SECTION_ID, GEOJSON)
            VALUES (v_area_id, c.GROUP_ID, 'CAMPSITE', c.NAME, NULL, NULL,
                    pg_temp.demo_rect(c.LAT, c.LON, 0, 0, 240, 125));
            -- reception building, south of the campsite
            INSERT INTO LOCATION (ID, NAME, STREET, HOUSE_NUMBER, ZIPCODE, CITY, REGION, COUNTRY_ISO, LATITUDE,
                                  LONGITUDE, NOTE, DELETED, CAMPSITE_CODE)
            VALUES (900000000 + c.NO * 100, c.NAME || ' reception', c.STREET, c.HOUSE, c.ZIP, c.CITY, c.REGION,
                    c.COUNTRY, ROUND(c.LAT - 10 / 111320.0, 10),
                    ROUND(c.LON + 120 / (111320 * COS(RADIANS(c.LAT))::NUMERIC), 10), 'Reception', FALSE, c.CODE);
            INSERT INTO RECEPTION_HOURS (ID, LOCATION_ID, DAY_OF_WEEK, OPEN_FROM, OPEN_TO)
            SELECT 900000000 + c.NO * 100 + d.N, 900000000 + c.NO * 100, d.DAY, TIME '08:00',
                   CASE WHEN d.N >= 6 THEN TIME '13:00' ELSE TIME '20:00' END
              FROM (VALUES (1, 'MONDAY'), (2, 'TUESDAY'), (3, 'WEDNESDAY'), (4, 'THURSDAY'), (5, 'FRIDAY'),
                           (6, 'SATURDAY'), (7, 'SUNDAY')) AS d(N, DAY);

            FOR v_section IN 0..1 LOOP
                v_sec_id := c.SECTION_ID + v_section;
                v_area_id := v_area_id + 1;
                INSERT INTO CAMPSITE_MAP_AREA (ID, SITE_GROUP_ID, AREA_TYPE, NAME, PITCH_NO, POWER_SECTION_ID,
                                               GEOJSON)
                VALUES (v_area_id, c.GROUP_ID, 'SECTION', 'Section ' || CHR(65 + v_section), NULL, v_sec_id,
                        pg_temp.demo_rect(c.LAT, c.LON, v_section * 120, 0, v_section * 120 + 120, 125));
                FOR v_row IN 0..2 LOOP
                    v_ped_no := v_section * 3 + v_row + 1;
                    v_ped_id := 900000000 + c.NO * 100 + v_ped_no;
                    v_loc_id := 900000000 + c.NO * 100 + 10 + v_ped_no;
                    v_code := c.CODE || '-P0' || v_ped_no;
                    v_type := 900000001 + (v_row + c.NO) % 3;
                    v_x := v_section * 120 + 60;
                    v_y := 5 + v_row * 40 + 32;
                    INSERT INTO LOCATION (ID, NAME, STREET, HOUSE_NUMBER, ZIPCODE, CITY, REGION, COUNTRY_ISO,
                                          LATITUDE, LONGITUDE, NOTE, DELETED, CAMPSITE_CODE)
                    VALUES (v_loc_id, c.NAME || ' ' || v_code, c.STREET, c.HOUSE, c.ZIP, c.CITY, c.REGION, c.COUNTRY,
                            ROUND(c.LAT + v_y / 111320, 10),
                            ROUND(c.LON + v_x / (111320 * COS(RADIANS(c.LAT))::NUMERIC), 10),
                            'Section ' || CHR(65 + v_section) || ', row ' || (v_row + 1), FALSE, c.CODE);
                    INSERT INTO PEDESTAL (ID, CODE, PEDESTAL_TYPE_ID, LOCATION_ID, GROUP_ID, REFERENCE_ID,
                                          SERIAL_NUMBER, FIRMWARE_VERSION, MODEM_ICCID, YEAR_OF_INSTALLATION,
                                          DESCRIPTION, PRICE_INFORMATION, BILLABLE, SETTLED_BY_CAMPSITE,
                                          LISTED_IN_APP, AUTOMATED_WHITELIST_UPDATES, MANAGED_EXTERNALLY,
                                          REMOTE_CONTROL, DELETED, LINK_URL)
                    VALUES (v_ped_id, v_code, v_type, v_loc_id, c.GROUP_ID, 'REF-' || v_code,
                            'SN' || (2024000 + c.NO * 100 + v_ped_no),
                            CASE v_type WHEN 900000001 THEN '1.8.3' WHEN 900000002 THEN '2.4.0' ELSE '3.1.2' END,
                            '8938501' || LPAD((c.NO * 100 + v_ped_no)::TEXT, 12, '0'),
                            DATE '2024-04-01' + (v_ped_no * 30),
                            'Row ' || (v_row + 1) || ' of section ' || CHR(65 + v_section),
                            'Power from 0.45 EUR/kWh', TRUE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE,
                            CASE WHEN v_type = 900000001 THEN NULL
                                 ELSE 'http://' || LOWER(v_code) || '.pedestals.campsite-power.example:8080' END);
                    FOR v_col IN 0..3 LOOP
                        v_pitch := CHR(65 + v_section) || LPAD((v_row * 4 + v_col + 1)::TEXT, 2, '0');
                        v_sock_id := 900010000 + (c.NO * 100 + v_ped_no) * 10 + v_col + 1;
                        INSERT INTO SOCKET (ID, PEDESTAL_ID, SOCKET_TYPE_ID, NAME, POS, SOCKET_NO, SOCKET_CODE,
                                            PITCH_NO, METER_KWH, STATUS, MOD_DATE, LAST_STATUS_UPDATE,
                                            FIRMWARE_VERSION, MONITORING, FREE_SOCKET, DELETED, POWER_SECTION_ID)
                        VALUES (v_sock_id, v_ped_id,
                                CASE WHEN v_col = 3 AND v_row = 2 THEN 2
                                     WHEN v_col = 0 AND c.NO = 3 AND v_row = 0 THEN 3
                                     ELSE 1 END,
                                v_code || '_' || (v_col + 1), v_col + 1, v_col + 1,
                                c.CODE || '*P0' || v_ped_no || '*' || (v_col + 1), v_pitch,
                                1000 + (v_sock_id % 97) * 37, 'AVAILABLE', now() - INTERVAL '46 days',
                                now() - INTERVAL '46 days', NULL, TRUE, FALSE, FALSE, v_sec_id);
                        v_area_id := v_area_id + 1;
                        INSERT INTO CAMPSITE_MAP_AREA (ID, SITE_GROUP_ID, AREA_TYPE, NAME, PITCH_NO,
                                                       POWER_SECTION_ID, GEOJSON)
                        VALUES (v_area_id, c.GROUP_ID, 'PITCH', 'Pitch ' || v_pitch, v_pitch, v_sec_id,
                                pg_temp.demo_rect(c.LAT, c.LON, v_section * 120 + 2.5 + v_col * 30, 5 + v_row * 40,
                                                  v_section * 120 + 27.5 + v_col * 30, 35 + v_row * 40));
                    END LOOP;
                    INSERT INTO PEDESTAL_STATE (ID, PEDESTAL_ID, APPROX_LAST_INCOMING, LAST_TIMEOUT, TIMEOUT)
                    VALUES (v_ped_id, v_ped_id, now() - ((v_ped_no * 2 + c.NO) || ' minutes')::INTERVAL, NULL,
                            FALSE);
                END LOOP;
            END LOOP;
        END LOOP;
    END
$$;

-- one pedestal in Ardeche has stopped talking (heartbeat timeout) since yesterday
UPDATE PEDESTAL_STATE
   SET APPROX_LAST_INCOMING = now() - INTERVAL '31 hours',
       LAST_TIMEOUT         = now() - INTERVAL '30 hours',
       TIMEOUT              = TRUE
 WHERE PEDESTAL_ID = 900000306;

INSERT INTO LOCAL_LIST_VERSION (ID, PEDESTAL_ID, VERSION, MOD_DATE)
SELECT p.ID, p.ID, 3 + (p.ID % 5)::INTEGER, now() - ((p.ID % 7) || ' days')::INTERVAL
  FROM PEDESTAL p
 WHERE p.ID >= 900000000
   AND p.PEDESTAL_TYPE_ID IN (900000001, 900000002);

-- tariffs: night price in Istria section B, weekend price on one Garda pedestal, nightly flat in Ardeche
INSERT INTO TARIFF (ID, NAME, DESCRIPTION, MARKET, CURRENCY, DEFAULT_TARIFF, LUD) VALUES
    (900000001, 'HR NIGHT SAVER', 'Cheaper power from 22:00 to 06:00, a connection fee per supply', 'HR', 'EUR', FALSE,
     now() - INTERVAL '90 days'),
    (900000002, 'IT WEEKEND', 'Weekend price on the lake front pitches', 'IT', 'EUR', FALSE,
     now() - INTERVAL '90 days'),
    (900000003, 'FR NIGHTLY FLAT', 'Price per night plus a low energy price up to 15 kWh', 'FR', 'EUR', FALSE,
     now() - INTERVAL '90 days');
INSERT INTO TARIFF_ELEMENT (ID, TARIFF_ID, POS) VALUES
    (900000001, 900000001, 0), (900000002, 900000001, 1),
    (900000003, 900000002, 0), (900000004, 900000002, 1),
    (900000005, 900000003, 0), (900000006, 900000003, 1);
INSERT INTO PRICE_COMPONENT (ID, TARIFF_ELEMENT_ID, DIMENSION_TYPE, PRICE, STEP_SIZE) VALUES
    (900000001, 900000001, 'ENERGY', 0.3200, 1),
    (900000002, 900000002, 'ENERGY', 0.4800, 1),
    (900000003, 900000002, 'FLAT', 0.5000, 1),
    (900000004, 900000003, 'ENERGY', 0.6200, 1),
    (900000005, 900000004, 'ENERGY', 0.5200, 1),
    (900000006, 900000005, 'ENERGY', 0.2500, 1),
    (900000007, 900000005, 'PER_NIGHT', 3.5000, 1),
    (900000008, 900000006, 'ENERGY', 0.4500, 1),
    (900000009, 900000006, 'PER_NIGHT', 3.5000, 1);
INSERT INTO TARIFF_RESTRICTION (ID, TARIFF_ELEMENT_ID, START_TIME, END_TIME, START_DATE, END_DATE, MIN_KWH, MAX_KWH)
VALUES
    (900000001, 900000001, TIME '22:00', TIME '06:00', NULL, NULL, NULL, NULL),
    (900000002, 900000003, NULL, NULL, NULL, NULL, NULL, NULL),
    (900000003, 900000005, NULL, NULL, NULL, NULL, NULL, 15);
INSERT INTO TARIFF_RESTRICTION_DAY (RESTRICTION_ID, DAY_OF_WEEK) VALUES
    (900000002, 'SATURDAY'), (900000002, 'SUNDAY');

INSERT INTO TARIFF_APPLICABILITY (ID, TARIFF_ID, SOCKET_ID, PEDESTAL_ID, START_DATE, CREATED_BY, CREATE_DATE)
SELECT p.ID, 900000001, NULL, p.ID, now() - INTERVAL '60 days', 'admin', now() - INTERVAL '60 days'
  FROM PEDESTAL p
 WHERE p.ID BETWEEN 900000104 AND 900000106;
INSERT INTO TARIFF_APPLICABILITY (ID, TARIFF_ID, SOCKET_ID, PEDESTAL_ID, START_DATE, CREATED_BY, CREATE_DATE)
SELECT p.ID, 900000003, NULL, p.ID, now() - INTERVAL '60 days', 'admin.fr', now() - INTERVAL '60 days'
  FROM PEDESTAL p
 WHERE p.ID BETWEEN 900000301 AND 900000306;
INSERT INTO TARIFF_APPLICABILITY (ID, TARIFF_ID, SOCKET_ID, PEDESTAL_ID, START_DATE, CREATED_BY, CREATE_DATE)
SELECT s.ID, 900000002, s.ID, NULL, now() - INTERVAL '60 days', 'host', now() - INTERVAL '60 days'
  FROM SOCKET s
 WHERE s.PEDESTAL_ID = 900000201;

INSERT INTO GRP_ATTR_DEFAULT_TARIFF (ID, GROUP_ID, TARIFF_ID) VALUES
    (900000061, 900000003, 900000003);

-- issuer partner exchanging cards and records with the network
INSERT INTO ISSUER_PARTNER (ID, NAME, DISPLAY_NAME, ISSUER_GROUP_ID, SITE_GROUP_ID, INTERFACE_VERSION, ENDPOINT,
                            CARD_IMPORT, CARD_EXPORT, RECORD_EXPORT, RECORD_IMPORT, ACTIVE, DELETED, CREATE_DATE)
VALUES (900000001, 'ADRIA CLUB', 'Adria Camping Club', 900000011, NULL, 'V2',
        'https://exchange.adria-club.example/api/v2', TRUE, FALSE, TRUE, FALSE, TRUE, FALSE,
        now() - INTERVAL '150 days');
INSERT INTO ISSUER_SYNC_STATUS (ID, ISSUER_PARTNER_ID, DIRECTION, STATUS, LAST_RUN, LAST_SUCCESS, ITEMS_ACCEPTED,
                                ITEMS_REJECTED, MESSAGE) VALUES
    (900000001, 900000001, 'CARD_IMPORT', 'OK', now() - INTERVAL '35 minutes', now() - INTERVAL '35 minutes', 42, 1,
     '42 cards imported, 1 card without a valid UID refused'),
    (900000002, 900000001, 'RECORD_EXPORT', 'ERROR', now() - INTERVAL '35 minutes', now() - INTERVAL '2 days', 0, 0,
     'Partner answered HTTP 503 Service Unavailable');
INSERT INTO GRP_ATTR_WHITELIST_FILTER (ID, GROUP_ID, SYNCHRONIZABLE, WHITELIST_FILTER_TYPE, ISSUER_PARTNER_ID) VALUES
    (900000071, 900000011, TRUE, 'IMPORT_EXPORT', 900000001);
