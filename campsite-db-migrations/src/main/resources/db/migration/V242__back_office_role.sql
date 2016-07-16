-- Back office (console tabs 8-10 and 13-15): review of consumption records, tariffs, QR payments, reports, ERP billing
-- and the issuer exchange. BILLING_EDIT allows the state changes of these tabs (billing runs and retries, re-pricing
-- and re-queueing records, cancelling stuck QR payments, editing issuer partners and re-syncing cards); market
-- administration has it too. Linked by name, the ids come from the sequence.

INSERT INTO USERRIGHT (NAME) VALUES ('BILLING_EDIT');

INSERT INTO USERROLE (NAME, GROUP_TYPES, SORT_KEY, SELF_SERVICE) VALUES ('BACK_OFFICE', 'STAFF,SITE,ISSUER', 60, FALSE);

INSERT INTO USERROLE_USERRIGHT (ROLE_ID, RIGHT_ID)
SELECT r.ID, ur.ID
  FROM USERROLE r, USERRIGHT ur
 WHERE r.NAME = 'BACK_OFFICE'
   AND ur.NAME IN ('PEDESTAL_VIEW', 'CARD_VIEW', 'GUEST_VIEW', 'TARIFF_EDIT', 'REPORT_VIEW', 'PRICE_REPORT_VIEW',
                   'CONSUMPTION_RECORD_REVIEW', 'BILLING_EDIT');

INSERT INTO USERROLE_USERRIGHT (ROLE_ID, RIGHT_ID)
SELECT r.ID, ur.ID
  FROM USERROLE r, USERRIGHT ur
 WHERE r.NAME = 'MARKET_ADMIN'
   AND ur.NAME = 'BILLING_EDIT';

INSERT INTO SECURITYGROUP (NAME) VALUES ('BACK OFFICE');

INSERT INTO SECURITYGROUP_USERROLE (SECURITYGROUP_ID, USERROLE_ID)
SELECT sg.ID, r.ID
  FROM SECURITYGROUP sg, USERROLE r
 WHERE sg.NAME = 'BACK OFFICE'
   AND r.NAME = 'BACK_OFFICE';
