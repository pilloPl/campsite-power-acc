-- Indexes of the consumption reports: records of the sockets of a power section, and the kWh accumulated per
-- pedestal over months, quarters and years.

CREATE INDEX IDX_CONSUMPTION_RECORD_SOCKET ON CONSUMPTION_RECORD (SOCKET_ID, PLUG_OUT);
CREATE INDEX IDX_CONSUMPTION_RECORD_PEDESTAL ON CONSUMPTION_RECORD (PEDESTAL_CODE, PLUG_OUT);
