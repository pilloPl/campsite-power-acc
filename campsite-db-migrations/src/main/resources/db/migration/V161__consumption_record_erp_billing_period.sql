-- The billing period of the ERP run that billed a consumption record (null while not billed). The run marks the
-- records it billed (EXPORT_STATUS WORK) with its period; the ERP file and the ERP's answers move them on to
-- TRANSFERRED, PROCESSED or ERROR together with the lines of their billing account. The kWh of a seasonal contract
-- already billed are the records with a period: the package is used up across the months of the season.

ALTER TABLE CONSUMPTION_RECORD ADD COLUMN ERP_BILLING_PERIOD VARCHAR(7);

CREATE INDEX IDX_CONSUMPTION_RECORD_ERP_BILLING ON CONSUMPTION_RECORD (BILLING_ACCOUNT_NO, ERP_BILLING_PERIOD);
