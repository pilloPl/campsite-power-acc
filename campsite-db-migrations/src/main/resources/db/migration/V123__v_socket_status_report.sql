-- Status report of the sockets: seconds per status in one month, from the monitoring log.
-- Every log row is paired with the next row of the same ordering (two copies of the numbered log, joined over
-- row number and row number - 1); the time up to the next row counts for the status of the row. A SUPPLYING row
-- whose next row of the same socket has the same meter reading counts as SUPPLYING_WITHOUT_CONSUMPTION (a device is
-- plugged in, the meter stands). The last row of a socket in the month has no successor and does not count.
-- socket_status_report(p_month) reports the month of the given day; V_SOCKET_STATUS_REPORT is the current month.

CREATE OR REPLACE FUNCTION socket_status_report(p_month DATE)
    RETURNS TABLE
            (
                ID              BIGINT,
                SOCKET_ID       BIGINT,
                SOCKET_NAME     VARCHAR,
                PEDESTAL_ID     BIGINT,
                GROUP_ID        BIGINT,
                TIME_IN_SECONDS BIGINT,
                STATUS          VARCHAR
            )
    LANGUAGE SQL
    STABLE
AS
$$
SELECT row_number() OVER (ORDER BY res.socket_id, res.status) AS id,
       res.socket_id,
       res.socket_name,
       res.pedestal_id,
       res.group_id,
       res.time_in_seconds,
       res.status
  FROM (SELECT b1.s_id   AS socket_id,
               b1.s_name AS socket_name,
               b1.p_id   AS pedestal_id,
               b1.g_id   AS group_id,
               SUM(TRUNC(CASE
                             WHEN b2.create_date1 >= b1.create_date AND b2.s_id1 = b1.s_id
                                 THEN EXTRACT(EPOCH FROM (b2.create_date1 - b1.create_date))
                             ELSE 0
                         END))::BIGINT AS time_in_seconds,
               CASE
                   WHEN b1.status = 'SUPPLYING'
                       AND b1.s_id = b2.s_id1
                       AND b1.meter_kwh = b2.meter_kwh1
                       THEN 'SUPPLYING_WITHOUT_CONSUMPTION'
                   ELSE b1.status
               END::VARCHAR AS status
          FROM (SELECT row_number() OVER (ORDER BY a1.s_id, a1.s_name, a1.create_date) AS a1_rownum,
                       a1.*
                  FROM (SELECT s.ID           AS s_id,
                               s.NAME         AS s_name,
                               p.ID           AS p_id,
                               g.ID           AS g_id,
                               l.CREATE_DATE  AS create_date,
                               l.STATUS       AS status,
                               l.METER_KWH    AS meter_kwh
                          FROM SOCKET_STATUS_LOG l
                          JOIN SOCKET s ON s.ID = l.SOCKET_ID
                          JOIN PEDESTAL p ON p.ID = s.PEDESTAL_ID
                          LEFT JOIN TBL_GROUP g ON g.ID = p.GROUP_ID
                         WHERE l.CREATE_DATE >= date_trunc('month', p_month)
                           AND l.CREATE_DATE <= date_trunc('month', p_month) + INTERVAL '1 month' - INTERVAL '1 second'
                           AND l.STATUS IN ('AVAILABLE', 'SUPPLYING', 'BREAKER_TRIPPED', 'FAULTED')) a1) b1,
               (SELECT row_number() OVER (ORDER BY a2.s_id1, a2.s_name1, a2.create_date1) - 1 AS a2_rownum,
                       a2.*
                  FROM (SELECT s.ID           AS s_id1,
                               s.NAME         AS s_name1,
                               p.ID           AS p_id1,
                               g.ID           AS g_id1,
                               l.CREATE_DATE  AS create_date1,
                               l.STATUS       AS status1,
                               l.METER_KWH    AS meter_kwh1
                          FROM SOCKET_STATUS_LOG l
                          JOIN SOCKET s ON s.ID = l.SOCKET_ID
                          JOIN PEDESTAL p ON p.ID = s.PEDESTAL_ID
                          LEFT JOIN TBL_GROUP g ON g.ID = p.GROUP_ID
                         WHERE l.CREATE_DATE >= date_trunc('month', p_month)
                           AND l.CREATE_DATE <= date_trunc('month', p_month) + INTERVAL '1 month' - INTERVAL '1 second'
                           AND l.STATUS IN ('AVAILABLE', 'SUPPLYING', 'BREAKER_TRIPPED', 'FAULTED')) a2) b2
         WHERE b1.a1_rownum = b2.a2_rownum
         GROUP BY b1.s_id,
                  b1.s_name,
                  b1.p_id,
                  b1.g_id,
                  CASE
                      WHEN b1.status = 'SUPPLYING'
                          AND b1.s_id = b2.s_id1
                          AND b1.meter_kwh = b2.meter_kwh1
                          THEN 'SUPPLYING_WITHOUT_CONSUMPTION'
                      ELSE b1.status
                  END
         ORDER BY b1.s_id) res
$$;

CREATE VIEW V_SOCKET_STATUS_REPORT AS
SELECT r.ID, r.SOCKET_ID, r.SOCKET_NAME, r.PEDESTAL_ID, r.GROUP_ID, r.TIME_IN_SECONDS, r.STATUS
  FROM socket_status_report(CURRENT_DATE) r;
