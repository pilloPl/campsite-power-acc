-- Intervals of the ERP jobs and the day of the month the billing of the month before starts.

INSERT INTO SYSTEM_PROPERTY (PROPERTY_KEY, PROPERTY_VALUE, DESCRIPTION) VALUES
    ('ErpAccountExportJobIntervalInSeconds', '900', 'Interval of the job handing orders of new guests to the ERP'),
    ('ErpAccountImportJobIntervalInSeconds', '900', 'Interval of the job taking the partner and account numbers of the ERP'),
    ('ErpExportJobIntervalInSeconds', '3600', 'Interval of the job writing the billing lines and taking the ERP booking answers'),
    ('ErpBillingRunDayOfMonth', '3', 'Day of the month from which the month before is billed to the ERP');
