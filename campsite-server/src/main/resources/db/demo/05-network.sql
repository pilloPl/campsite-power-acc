-- Demo data (profile demo): the wider network around the three campsites with supplies. Four markets (HR, IT, FR
-- and ES) with regions, campsite operators and 16 campsites in the SITE tree, card issuers with sub-groups and a
-- dense acceptance agreement graph in the ISSUER tree, reception and service teams in the STAFF tree, and group
-- attributes on several levels, so that the effective values of a group show what it inherits and what it
-- overrides. The 13 new campsites have a map outline, two power sections and two pedestals with four sockets each;
-- they have no guests or supplies. Sections are power sections and map areas of a campsite, not groups: the
-- pedestal's group is the campsite for the map and the scope rules.

-- SITE tree: market -> region -> operator -> campsite (some campsites directly under their region)
INSERT INTO TBL_GROUP (ID, PARENT_ID, NAME, TYPENAME, DISPLAYNAME) VALUES
    (900000101, 11, 'HR REGION ISTRIA', 'SITE', 'Istria'),
    (900000102, 11, 'HR REGION DALMATIA', 'SITE', 'Dalmatia'),
    (900000103, 12, 'IT REGION VENETO', 'SITE', 'Veneto'),
    (900000104, 12, 'IT REGION TUSCANY', 'SITE', 'Tuscany'),
    (900000105, 13, 'FR REGION AUVERGNE RHONE ALPES', 'SITE', 'Auvergne-Rhone-Alpes'),
    (900000106, 13, 'FR REGION OCCITANIE', 'SITE', 'Occitanie'),
    (900000107, 14, 'ES REGION CATALONIA', 'SITE', 'Catalonia'),
    (900000108, 14, 'ES REGION VALENCIA', 'SITE', 'Valencian Community'),
    (900000111, 900000101, 'ADRIATIC HOLIDAY PARKS', 'SITE', 'Adriatic Holiday Parks'),
    (900000112, 900000102, 'DALMATIA CAMPS', 'SITE', 'Dalmatia Camps'),
    (900000113, 900000103, 'LAKE AND SEA CAMPING', 'SITE', 'Lake & Sea Camping'),
    (900000114, 900000105, 'GORGES ET RIVIERES', 'SITE', 'Gorges & Rivieres Campings'),
    (900000115, 900000107, 'COSTA CAMPINGS', 'SITE', 'Costa Campings'),
    (900000121, 900000111, 'POREC PINE GROVE', 'SITE', 'Camping Porec Pine Grove'),
    (900000122, 900000101, 'ROVINJ PORTON', 'SITE', 'Camping Rovinj Porton'),
    (900000123, 900000112, 'ZATON BAY', 'SITE', 'Zaton Bay Holiday Camp'),
    (900000125, 900000113, 'CAVALLINO BEACH', 'SITE', 'Cavallino Beach Camping'),
    (900000126, 900000104, 'ELBA LACONA', 'SITE', 'Camping Elba Lacona'),
    (900000127, 900000104, 'MAREMMA PINES', 'SITE', 'Maremma Pines Camping'),
    (900000128, 900000114, 'ANNECY LAKE', 'SITE', 'Camping du Lac d''Annecy'),
    (900000129, 900000106, 'CANET PLAGE', 'SITE', 'Camping Canet Plage'),
    (900000130, 900000106, 'CAMARGUE FLAMANTS', 'SITE', 'Camping des Flamants Camargue'),
    (900000131, 900000115, 'COSTA BRAVA PALS', 'SITE', 'Camping Costa Brava Pals'),
    (900000132, 900000115, 'SALOU PALMERAS', 'SITE', 'Camping Salou Palmeras'),
    (900000133, 900000108, 'BENIDORM SOL', 'SITE', 'Camping Benidorm Sol'),
    (900000134, 900000108, 'DENIA NARANJOS', 'SITE', 'Camping Denia Naranjos');

-- the campsites with supplies move under their region and operator
UPDATE TBL_GROUP SET PARENT_ID = 900000111 WHERE ID = 900000001;
UPDATE TBL_GROUP SET PARENT_ID = 900000113 WHERE ID = 900000002;
UPDATE TBL_GROUP SET PARENT_ID = 900000114 WHERE ID = 900000003;

-- ISSUER tree: issuers per market with sub-groups, two international issuers (home market of their head office)
INSERT INTO TBL_GROUP (ID, PARENT_ID, NAME, TYPENAME, DISPLAYNAME) VALUES
    (900000014, 4, 'ES NETWORK GUESTS', 'ISSUER', 'Network guests Spain'),
    (900000201, 900000011, 'ADRIA CLUB FAMILY', 'ISSUER', 'Adria Club Family'),
    (900000202, 900000011, 'ADRIA CLUB YOUTH', 'ISSUER', 'Adria Club Youth'),
    (900000203, 1, 'DALMATIA CARAVAN CLUB', 'ISSUER', 'Dalmatia Caravan Club'),
    (900000204, 2, 'CAMPEGGIO CLUB ITALIA', 'ISSUER', 'Campeggio Club Italia'),
    (900000205, 900000204, 'CLUB ITALIA GOLD', 'ISSUER', 'Club Italia Gold'),
    (900000206, 3, 'CLUB CAMPING FRANCE', 'ISSUER', 'Club Camping France'),
    (900000207, 3, 'PROVENCE ROUTARDS', 'ISSUER', 'Provence Routards'),
    (900000208, 4, 'CLUB CAMPING IBERICO', 'ISSUER', 'Club Camping Iberico'),
    (900000209, 900000208, 'IBERICO SENIOR', 'ISSUER', 'Iberico Senior'),
    (900000210, 1, 'EURO CAMPING CARD', 'ISSUER', 'Euro Camping Card (international)'),
    (900000211, 3, 'NORDIC CARAVAN CARD', 'ISSUER', 'Nordic Caravan Card (international)');

-- acceptance agreements: an agreement with a region or operator covers its campsites; a sub-group with its own list
-- replaces the list of its parent (Adria Club Youth, Club Italia Gold), the others inherit it
INSERT INTO GROUP_ASSOCIATEDGROUP (GROUP_ID, ASSOCIATEDGROUP_ID) VALUES
    (31, 900000104), (31, 900000107), (31, 900000108), (31, 900000106),
    (900000011, 900000101), (900000011, 900000102),
    (900000202, 900000122),
    (900000203, 900000102), (900000203, 900000122),
    (900000012, 900000103), (900000012, 900000104),
    (900000204, 900000103), (900000204, 900000104), (900000204, 900000101),
    (900000205, 900000002), (900000205, 900000125), (900000205, 900000126), (900000205, 900000128),
    (900000013, 900000105), (900000013, 900000106), (900000013, 900000131),
    (900000206, 900000106), (900000206, 900000107),
    (900000207, 900000130),
    (900000014, 900000107), (900000014, 900000108), (900000014, 900000129),
    (900000208, 900000108), (900000208, 900000107),
    (900000209, 900000133),
    (900000210, 900000111), (900000210, 900000113), (900000210, 900000114), (900000210, 900000115),
    (900000211, 900000128), (900000211, 900000002), (900000211, 900000123);

-- STAFF tree: a reception per campsite under the market reception, a service team and the back office per market
INSERT INTO TBL_GROUP (ID, PARENT_ID, NAME, TYPENAME, DISPLAYNAME) VALUES
    (900000301, 71, 'ISTRIA BAY RECEPTION', 'STAFF', 'Reception Camp Istria Bay'),
    (900000302, 71, 'POREC PINE GROVE RECEPTION', 'STAFF', 'Reception Porec Pine Grove'),
    (900000303, 71, 'ZATON BAY RECEPTION', 'STAFF', 'Reception Zaton Bay'),
    (900000304, 72, 'GARDA LAKESIDE RECEPTION', 'STAFF', 'Reception Garda Lakeside'),
    (900000305, 72, 'ELBA LACONA RECEPTION', 'STAFF', 'Reception Elba Lacona'),
    (900000306, 73, 'ARDECHE RIVER RECEPTION', 'STAFF', 'Reception Ardeche River'),
    (900000307, 73, 'CANET PLAGE RECEPTION', 'STAFF', 'Reception Canet Plage'),
    (900000308, 74, 'COSTA BRAVA PALS RECEPTION', 'STAFF', 'Reception Costa Brava Pals'),
    (900000309, 74, 'BENIDORM SOL RECEPTION', 'STAFF', 'Reception Benidorm Sol'),
    (900000311, 21, 'HR SERVICE TEAM', 'STAFF', 'Croatia pedestal service'),
    (900000312, 22, 'IT SERVICE TEAM', 'STAFF', 'Italy pedestal service'),
    (900000313, 23, 'FR SERVICE TEAM', 'STAFF', 'France pedestal service'),
    (900000314, 24, 'ES SERVICE TEAM', 'STAFF', 'Spain pedestal service'),
    (900000321, 21, 'HR BACK OFFICE', 'STAFF', 'Croatia back office'),
    (900000324, 24, 'ES BACK OFFICE', 'STAFF', 'Spain back office');

-- the demo users belong to their desk or team; admin.es administers the Spanish market
UPDATE USERINFO SET GROUP_ID = 900000301 WHERE ID IN (900000001, 900000009);
UPDATE USERINFO SET GROUP_ID = 900000304 WHERE ID = 900000002;
UPDATE USERINFO SET GROUP_ID = 900000311 WHERE ID = 900000003;
UPDATE USERINFO SET GROUP_ID = 900000321 WHERE ID = 900000004;
INSERT INTO USERINFO (ID, USERNAME, PASSWORD_HASH, NAME, EMAIL, STATUS, EMAIL_LOCALE, GROUP_ID, CREATE_DATE, DELETED)
VALUES (900000010, 'admin.es', NULL, 'Lucia Admin', 'admin.es@campsite-power.example', 'ACTIVE', 'es', 24,
        now() - INTERVAL '90 days', FALSE);
INSERT INTO USERINFO_USERROLE (USERINFO_ID, USERROLE_ID)
SELECT 900000010, r.ID FROM USERROLE r WHERE r.NAME = 'MARKET_ADMIN';
INSERT INTO USERINFO_GROUP (USERINFO_ID, GROUP_ID) VALUES
    (900000010, 24), (900000010, 14), (900000010, 4);

-- group attributes on several levels (the nearest ancestor-or-self wins)
INSERT INTO GRP_ATTR_SITE_TENANT (ID, GROUP_ID, NAME) VALUES
    (900000101, 900000111, 'Adriatic Holiday Parks d.d.'),
    (900000102, 900000112, 'Dalmatia Camps d.o.o.'),
    (900000103, 900000113, 'Lake & Sea Camping S.p.A.'),
    (900000104, 900000114, 'Gorges et Rivieres SAS'),
    (900000105, 900000115, 'Costa Campings S.L.'),
    (900000106, 900000104, 'Toscana Campeggi S.r.l.'),
    (900000107, 900000106, 'Occitanie Plein Air SARL'),
    (900000108, 900000108, 'Campings Levante S.L.'),
    (900000109, 900000122, 'Porton Biondi d.o.o.');
INSERT INTO GRP_ATTR_ISSUER_TENANT (ID, GROUP_ID, NAME) VALUES
    (900000111, 900000203, 'Dalmatia Caravan Club'),
    (900000112, 900000204, 'Campeggio Club Italia'),
    (900000113, 900000206, 'Club Camping France'),
    (900000114, 900000207, 'Provence Routards'),
    (900000115, 900000014, 'Campsite network Spain'),
    (900000116, 900000208, 'Club Camping Iberico'),
    (900000117, 900000210, 'Euro Camping Card GmbH'),
    (900000118, 900000211, 'Nordic Caravan Card AB'),
    (900000119, 900000202, 'Adria Club Youth');
INSERT INTO GRP_ATTR_NOTIFICATION (ID, GROUP_ID, NAME, EMAIL, NOTIFICATION_TYPE) VALUES
    (900000121, 900000101, 'Pedestal service Istria region', 'service.istria@campsite-power.example',
     'PEDESTAL_STATUS'),
    (900000122, 900000102, 'Pedestal service Dalmatia', 'service.dalmatia@campsite-power.example', 'PEDESTAL_STATUS'),
    (900000123, 12, 'Pedestal service Italy', 'service.italy@campsite-power.example', 'PEDESTAL_STATUS'),
    (900000124, 13, 'Pedestal service France', 'service.france@campsite-power.example', 'PEDESTAL_STATUS'),
    (900000125, 14, 'Pedestal service Spain', 'service.spain@campsite-power.example', 'PEDESTAL_STATUS'),
    (900000126, 900000107, 'Firmware Catalonia', 'firmware.catalonia@campsite-power.example', 'FIRMWARE_STATUS');
INSERT INTO GRP_ATTR_COMMLOG (ID, GROUP_ID, NAME, ACTIVE) VALUES
    (900000131, 11, 'Communication log Croatia', TRUE),
    (900000132, 900000104, 'Communication log Tuscany', FALSE),
    (900000133, 14, 'Communication log Spain', TRUE),
    (900000134, 900000133, 'Communication log Benidorm', FALSE);
INSERT INTO GRP_ATTR_LOCAL_LIST (ID, GROUP_ID, LOCAL_LIST_ALLOWED, INCLUDE_CARDS, INCLUDE_HOUSE_CARDS) VALUES
    (900000141, 900000111, TRUE, TRUE, FALSE),
    (900000142, 14, TRUE, TRUE, TRUE),
    (900000143, 900000132, FALSE, FALSE, FALSE);
INSERT INTO GRP_ATTR_EXPORT_CAMPSITE_NAME (ID, GROUP_ID, NAME) VALUES
    (900000151, 900000111, 'ADRIATIC PARKS'),
    (900000152, 900000121, 'POREC PINES'),
    (900000153, 900000115, 'COSTA CAMPINGS');
INSERT INTO GRP_ATTR_DEFAULT_TARIFF (ID, GROUP_ID, TARIFF_ID) VALUES
    (900000161, 900000104, 900000002),
    (900000162, 900000102, 900000001);
INSERT INTO GRP_ATTR_WELCOME_CARD_ADOPTION (ID, GROUP_ID, ALLOWED) VALUES
    (900000171, 900000108, TRUE),
    (900000172, 900000133, FALSE);
INSERT INTO GRP_ATTR_WHITELIST_FILTER (ID, GROUP_ID, SYNCHRONIZABLE, WHITELIST_FILTER_TYPE, ISSUER_PARTNER_ID) VALUES
    (900000072, 900000202, FALSE, 'EXPORT', 900000001),
    (900000073, 900000209, FALSE, 'IMPORT_EXPORT', NULL);

-- the new campsites: 120 m x 80 m with sections A (west) and B (east), one pedestal per section with 4 sockets for a
-- 2 x 2 block of pitches; link v2 and v3 pedestals (answered by the demo pedestal simulator)
DO
$$
    DECLARE
        c         RECORD;
        v_section INTEGER;
        v_col     INTEGER;
        v_ped_id  BIGINT;
        v_loc_id  BIGINT;
        v_type    BIGINT;
        v_code    TEXT;
        v_sec_id  BIGINT;
        v_pitch   TEXT;
        v_area_id BIGINT := 900005000;
        v_sock_id BIGINT;
        v_x       NUMERIC;
        v_y       NUMERIC;
    BEGIN
        FOR c IN
            SELECT *
              FROM (VALUES (4, 900000121::BIGINT, 'POREC', 'Camping Porec Pine Grove', 45.1966::NUMERIC,
                            13.5948::NUMERIC, 'Porec', '52440', 'Zelena Laguna', '12', 'Istria', 'HR'),
                           (5, 900000122, 'ROVINJ', 'Camping Rovinj Porton', 45.0930, 13.6390, 'Rovinj', '52210',
                            'Veli Jural', '3', 'Istria', 'HR'),
                           (6, 900000123, 'ZATON', 'Zaton Bay Holiday Camp', 44.2350, 15.1660, 'Zaton', '23232',
                            'Draznikova', '76', 'Zadar', 'HR'),
                           (7, 900000125, 'CAVALLINO', 'Cavallino Beach Camping', 45.4712, 12.5452,
                            'Cavallino-Treporti', '30013', 'Via Fausta', '258', 'Veneto', 'IT'),
                           (8, 900000126, 'ELBA', 'Camping Elba Lacona', 42.7598, 10.3072, 'Capoliveri', '57031',
                            'Localita Lacona', '1', 'Tuscany', 'IT'),
                           (9, 900000127, 'MAREMMA', 'Maremma Pines Camping', 42.7248, 10.9805,
                            'Marina di Grosseto', '58100', 'Via del Molo', '8', 'Tuscany', 'IT'),
                           (10, 900000128, 'ANNECY', 'Camping du Lac d''Annecy', 45.7868, 6.2203, 'Doussard',
                            '74210', 'Route du Lac', '20', 'Auvergne-Rhone-Alpes', 'FR'),
                           (11, 900000129, 'CANET', 'Camping Canet Plage', 42.6918, 3.0302, 'Canet-en-Roussillon',
                            '66140', 'Avenue de la Mer', '5', 'Occitanie', 'FR'),
                           (12, 900000130, 'CAMARGUE', 'Camping des Flamants Camargue', 43.4552, 4.4148,
                            'Saintes-Maries-de-la-Mer', '13460', 'Route d''Aigues-Mortes', '2', 'Occitanie', 'FR'),
                           (13, 900000131, 'PALS', 'Camping Costa Brava Pals', 41.9702, 3.1948, 'Pals', '17256',
                            'Carrer de la Platja', '11', 'Catalonia', 'ES'),
                           (14, 900000132, 'SALOU', 'Camping Salou Palmeras', 41.0782, 1.1498, 'Salou', '43840',
                            'Avinguda de la Mar', '24', 'Catalonia', 'ES'),
                           (15, 900000133, 'BENIDORM', 'Camping Benidorm Sol', 38.5498, -0.1102, 'Benidorm',
                            '03503', 'Avenida de la Comunitat', '40', 'Valencian Community', 'ES'),
                           (16, 900000134, 'DENIA', 'Camping Denia Naranjos', 38.8602, 0.0798, 'Denia', '03700',
                            'Carretera de Las Marinas', '9', 'Valencian Community', 'ES'))
                       AS t(NO, GROUP_ID, CODE, NAME, LAT, LON, CITY, ZIP, STREET, HOUSE, REGION, COUNTRY)
             ORDER BY 1
        LOOP
            v_area_id := v_area_id + 1;
            INSERT INTO CAMPSITE_MAP_AREA (ID, SITE_GROUP_ID, AREA_TYPE, NAME, PITCH_NO, POWER_SECTION_ID, GEOJSON)
            VALUES (v_area_id, c.GROUP_ID, 'CAMPSITE', c.NAME, NULL, NULL, pg_temp.demo_rect(c.LAT, c.LON, 0, 0, 120, 80));
            INSERT INTO LOCATION (ID, NAME, STREET, HOUSE_NUMBER, ZIPCODE, CITY, REGION, COUNTRY_ISO, LATITUDE,
                                  LONGITUDE, NOTE, DELETED, CAMPSITE_CODE)
            VALUES (900000000 + c.NO * 100, c.NAME || ' reception', c.STREET, c.HOUSE, c.ZIP, c.CITY, c.REGION,
                    c.COUNTRY, ROUND(c.LAT - 10 / 111320.0, 10),
                    ROUND(c.LON + 60 / (111320 * COS(RADIANS(c.LAT))::NUMERIC), 10), 'Reception', FALSE, c.CODE);
            FOR v_section IN 0..1 LOOP
                v_sec_id := 900000100 + c.NO * 2 + v_section;
                INSERT INTO POWER_SECTION (ID, SECTION_NAME, MAX_CURRENT_A, ALGORITHM_BEAN_NAME)
                VALUES (v_sec_id, c.CODE || ' SECTION ' || CHR(65 + v_section), CASE v_section WHEN 0 THEN 63 ELSE 40 END,
                        'nominalCurrentSum');
                v_area_id := v_area_id + 1;
                INSERT INTO CAMPSITE_MAP_AREA (ID, SITE_GROUP_ID, AREA_TYPE, NAME, PITCH_NO, POWER_SECTION_ID,
                                               GEOJSON)
                VALUES (v_area_id, c.GROUP_ID, 'SECTION', 'Section ' || CHR(65 + v_section), NULL, v_sec_id,
                        pg_temp.demo_rect(c.LAT, c.LON, v_section * 60, 0, v_section * 60 + 60, 80));
                v_ped_id := 900000000 + c.NO * 100 + v_section + 1;
                v_loc_id := 900000000 + c.NO * 100 + 10 + v_section + 1;
                v_code := c.CODE || '-P0' || (v_section + 1);
                v_type := CASE WHEN (c.NO + v_section) % 2 = 0 THEN 900000002 ELSE 900000003 END;
                v_x := v_section * 60 + 30;
                v_y := 40;
                INSERT INTO LOCATION (ID, NAME, STREET, HOUSE_NUMBER, ZIPCODE, CITY, REGION, COUNTRY_ISO, LATITUDE,
                                      LONGITUDE, NOTE, DELETED, CAMPSITE_CODE)
                VALUES (v_loc_id, c.NAME || ' ' || v_code, c.STREET, c.HOUSE, c.ZIP, c.CITY, c.REGION, c.COUNTRY,
                        ROUND(c.LAT + v_y / 111320, 10), ROUND(c.LON + v_x / (111320 * COS(RADIANS(c.LAT))::NUMERIC), 10),
                        'Section ' || CHR(65 + v_section), FALSE, c.CODE);
                INSERT INTO PEDESTAL (ID, CODE, PEDESTAL_TYPE_ID, LOCATION_ID, GROUP_ID, REFERENCE_ID, SERIAL_NUMBER,
                                      FIRMWARE_VERSION, MODEM_ICCID, YEAR_OF_INSTALLATION, DESCRIPTION,
                                      PRICE_INFORMATION, BILLABLE, SETTLED_BY_CAMPSITE, LISTED_IN_APP,
                                      AUTOMATED_WHITELIST_UPDATES, MANAGED_EXTERNALLY, REMOTE_CONTROL, DELETED,
                                      LINK_URL)
                VALUES (v_ped_id, v_code, v_type, v_loc_id, c.GROUP_ID, 'REF-' || v_code,
                        'SN' || (2025000 + c.NO * 100 + v_section + 1),
                        CASE v_type WHEN 900000002 THEN '2.4.0' ELSE '3.1.2' END,
                        '8938502' || LPAD((c.NO * 100 + v_section + 1)::TEXT, 12, '0'), DATE '2025-04-01',
                        'Section ' || CHR(65 + v_section), 'Power from 0.42 EUR/kWh', TRUE, FALSE, TRUE, FALSE, FALSE,
                        TRUE, FALSE, 'http://' || LOWER(v_code) || '.pedestals.campsite-power.example:8080');
                FOR v_col IN 0..3 LOOP
                    v_pitch := CHR(65 + v_section) || LPAD((v_col + 1)::TEXT, 2, '0');
                    v_sock_id := 900010000 + (c.NO * 100 + v_section + 1) * 10 + v_col + 1;
                    INSERT INTO SOCKET (ID, PEDESTAL_ID, SOCKET_TYPE_ID, NAME, POS, SOCKET_NO, SOCKET_CODE, PITCH_NO,
                                        METER_KWH, STATUS, MOD_DATE, LAST_STATUS_UPDATE, FIRMWARE_VERSION,
                                        MONITORING, FREE_SOCKET, DELETED, POWER_SECTION_ID)
                    VALUES (v_sock_id, v_ped_id, 1, v_code || '_' || (v_col + 1), v_col + 1, v_col + 1,
                            c.CODE || '*P0' || (v_section + 1) || '*' || (v_col + 1), v_pitch,
                            200 + (v_sock_id % 89) * 23,
                            CASE WHEN c.NO = 14 AND v_section = 1 AND v_col = 2 THEN 'FAULTED'
                                 WHEN c.NO = 9 AND v_section = 0 AND v_col = 3 THEN 'BREAKER_TRIPPED'
                                 ELSE 'AVAILABLE' END,
                            now() - INTERVAL '3 days', now() - INTERVAL '3 days', NULL, TRUE, FALSE, FALSE, v_sec_id);
                    v_area_id := v_area_id + 1;
                    INSERT INTO CAMPSITE_MAP_AREA (ID, SITE_GROUP_ID, AREA_TYPE, NAME, PITCH_NO, POWER_SECTION_ID,
                                                   GEOJSON)
                    VALUES (v_area_id, c.GROUP_ID, 'PITCH', 'Pitch ' || v_pitch, v_pitch, v_sec_id,
                            pg_temp.demo_rect(c.LAT, c.LON, v_section * 60 + 3 + (v_col % 2) * 28,
                                              4 + (v_col / 2) * 40, v_section * 60 + 29 + (v_col % 2) * 28,
                                              36 + (v_col / 2) * 40));
                END LOOP;
                INSERT INTO PEDESTAL_STATE (ID, PEDESTAL_ID, APPROX_LAST_INCOMING, LAST_TIMEOUT, TIMEOUT)
                VALUES (v_ped_id, v_ped_id, now() - ((c.NO + v_section * 3) || ' minutes')::INTERVAL, NULL, FALSE);
            END LOOP;
        END LOOP;
    END
$$;

INSERT INTO LOCAL_LIST_VERSION (ID, PEDESTAL_ID, VERSION, MOD_DATE)
SELECT p.ID, p.ID, 1, now() - INTERVAL '3 days'
  FROM PEDESTAL p
 WHERE p.ID >= 900000400
   AND p.ID < 900001700
   AND p.PEDESTAL_TYPE_ID = 900000002;
