-- Summary of the self-service console users for the user lists: one row per user having a self-service role,
-- with flags for the affiliate owner and seasonal resident roles. Refreshed by a job
-- (UserinfoSummaryRefreshJobIntervalInSeconds, default 120 s).

CREATE MATERIALIZED VIEW USERINFO_SUMMARY AS
SELECT u.ID,
       u.USERNAME,
       u.NAME,
       u.EMAIL,
       u.STATUS,
       CASE WHEN (SELECT ur_owner.NAME
                    FROM USERINFO_USERROLE uu_owner
                    JOIN USERROLE ur_owner ON uu_owner.USERROLE_ID = ur_owner.ID
                   WHERE uu_owner.USERINFO_ID = u.ID
                     AND ur_owner.NAME = 'AFFILIATE_OWNER') IS NULL THEN FALSE ELSE TRUE END AS AFFILIATE_OWNER,
       CASE WHEN (SELECT ur_res.NAME
                    FROM USERINFO_USERROLE uu_res
                    JOIN USERROLE ur_res ON uu_res.USERROLE_ID = ur_res.ID
                   WHERE uu_res.USERINFO_ID = u.ID
                     AND ur_res.NAME = 'SEASONAL_RESIDENT') IS NULL THEN FALSE ELSE TRUE END AS SEASONAL_RESIDENT,
       u.GROUP_ID
  FROM USERINFO u
 WHERE u.DELETED = FALSE
   AND EXISTS (SELECT 1
                 FROM USERINFO_USERROLE uu
                 JOIN USERROLE ur ON uu.USERROLE_ID = ur.ID
                WHERE uu.USERINFO_ID = u.ID
                  AND ur.SELF_SERVICE = TRUE)
 ORDER BY u.USERNAME;

CREATE UNIQUE INDEX IDX_USERINFO_SUMMARY_ID ON USERINFO_SUMMARY (ID);
CREATE INDEX IDX_USERINFO_SUMMARY_GROUP ON USERINFO_SUMMARY (GROUP_ID);
