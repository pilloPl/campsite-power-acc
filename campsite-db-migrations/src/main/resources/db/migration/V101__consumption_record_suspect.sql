-- The review queue of consumption records: suspect records (implausible meter values or times) and records whose
-- price could not be calculated, until someone confirms or rejects them. A view on CONSUMPTION_RECORD, not a second
-- record of the session.

CREATE VIEW CONSUMPTION_RECORD_SUSPECT AS
SELECT cr.ID,
       cr.RECORD_NO,
       cr.SUPPLY_SESSION_ID,
       cr.PEDESTAL_CODE,
       cr.SOCKET_CODE,
       cr.CAMPSITE_CODE,
       cr.SITE_GROUP_ID,
       cr.ISSUER_GROUP_ID,
       cr.CARD_RFID,
       cr.GUEST_NAME,
       cr.PLUG_IN,
       cr.PLUG_OUT,
       cr.DURATION_S,
       cr.NIGHTS,
       cr.KWH,
       CASE WHEN cr.NIGHTS > 0 AND cr.KWH IS NOT NULL THEN ROUND(cr.KWH / cr.NIGHTS, 3) END AS KWH_PER_NIGHT,
       cr.VALIDATION_STATE,
       cr.SUSPECT_REASON,
       cr.PRICING_STATUS,
       cr.PRICING_FAILURE_REASON,
       cr.CREATE_DATE
  FROM CONSUMPTION_RECORD cr
 WHERE cr.EXPORT_STATUS = 'REVIEW'
   AND (cr.VALIDATION_STATE = 'SUSPECT' OR cr.PRICING_STATUS = 'PRICING_FAILED');
