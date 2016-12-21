-- Configuration rows of the record checks (until now only code defaults): after how many days a billable record
-- nobody exports is reported, and who reviews suspect records (empty: the developer address of DeveloperEmail).

INSERT INTO SYSTEM_PROPERTY (PROPERTY_KEY, PROPERTY_VALUE, DESCRIPTION) VALUES
    ('RecordWithoutExportAfterDays', '40',
     'A billable record still waiting for an export this many days after its plug-out is reported as without export'),
    ('SuspectRecordEmail', '',
     'Recipients of the mail about suspect consumption records; empty sends it to the developer address');
