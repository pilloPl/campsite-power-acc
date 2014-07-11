-- Right to confirm or reject records of the review queue (suspect records, failed pricing): market administration
-- and customer service. Linked by name, the ids come from the sequence.

INSERT INTO USERRIGHT (NAME) VALUES ('CONSUMPTION_RECORD_REVIEW');

INSERT INTO USERROLE_USERRIGHT (ROLE_ID, RIGHT_ID)
SELECT r.ID, ur.ID
  FROM USERROLE r, USERRIGHT ur
 WHERE r.NAME IN ('MARKET_ADMIN', 'CUSTOMER_SERVICE')
   AND ur.NAME = 'CONSUMPTION_RECORD_REVIEW';
