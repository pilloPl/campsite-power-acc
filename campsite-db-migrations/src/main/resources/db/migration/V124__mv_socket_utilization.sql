-- Utilization of the sockets for the occupancy reports: every monitoring log row with its 10 minute bucket, whether
-- the socket really supplied (SUPPLYING and the meter moved up to the next row) and the seconds up to the next row.
-- Refreshed by a job (SocketUtilizationRefreshJobIntervalInSeconds, default 3600 s).

CREATE MATERIALIZED VIEW SOCKET_UTILIZATION AS
SELECT ID,
       SOCKET_ID,
       GROUP_ID,
       STATUS,
       METER_KWH,
       CREATE_DATE                                              AS MOD_DATE,
       EXTRACT(HOUR FROM CREATE_DATE)::INTEGER                  AS HOUR,
       (FLOOR(EXTRACT(MINUTE FROM CREATE_DATE) / 10) * 10)::INTEGER AS MINUTES,
       CASE
           WHEN (AVG(METER_KWH) OVER (PARTITION BY SOCKET_ID ORDER BY CREATE_DATE
                                      ROWS BETWEEN 0 PRECEDING AND 1 FOLLOWING) - METER_KWH) = 0
               OR STATUS <> 'SUPPLYING'
               THEN 0
           ELSE 1
       END                                                      AS SUPPLYING,
       EXTRACT(EPOCH FROM (LAST_VALUE(CREATE_DATE) OVER (PARTITION BY SOCKET_ID ORDER BY CREATE_DATE
                                                        ROWS BETWEEN 0 PRECEDING AND 1 FOLLOWING)
           - CREATE_DATE))::BIGINT                              AS TIME_IN_SECONDS
  FROM (SELECT l.ID,
               l.SOCKET_ID,
               p.GROUP_ID,
               l.STATUS,
               l.METER_KWH,
               l.CREATE_DATE
          FROM SOCKET_STATUS_LOG l
          JOIN SOCKET s ON l.SOCKET_ID = s.ID
          JOIN PEDESTAL p ON s.PEDESTAL_ID = p.ID
          JOIN TBL_GROUP g ON p.GROUP_ID = g.ID
         WHERE l.STATUS IN ('AVAILABLE', 'SUPPLYING', 'BREAKER_TRIPPED', 'FAULTED')) monitored;

CREATE UNIQUE INDEX IDX_SOCKET_UTILIZATION_ID ON SOCKET_UTILIZATION (ID);
CREATE INDEX IDX_SOCKET_UTILIZATION_GROUP ON SOCKET_UTILIZATION (GROUP_ID, MOD_DATE);
