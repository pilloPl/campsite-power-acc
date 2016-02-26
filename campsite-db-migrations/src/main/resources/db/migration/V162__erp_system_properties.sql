-- ERP constants of the billing lines and the file exchange with the ERP. Inserted by key (ids from the sequence) so
-- that parallel migrations never collide.

INSERT INTO SYSTEM_PROPERTY (PROPERTY_KEY, PROPERTY_VALUE, DESCRIPTION) VALUES
    ('ErpArticleClassification', 'EL', 'Article classification of the billing lines sent to ERP'),
    ('ErpSalesOffice', 'CS01', 'Sales office of the billing lines sent to ERP'),
    ('ErpSalesGroup', 'PWR', 'Sales group of the billing lines sent to ERP'),
    ('ErpAssignee', 'CAMPSITE-POWER', 'Assignee of the billing lines sent to ERP'),
    ('ErpOrderType', 'ZPW', 'Order type of the billing lines sent to ERP'),
    ('ErpAlternateTextOrganisation', 'Pitch power of your organisation', 'Bill text for organisations'),
    ('ErpAlternateTextPrivate', 'Pitch power of your stay', 'Bill text for private guests'),
    ('ErpPowerArticleNo', 'ART-POWER-KWH', 'ERP article of the power of supplies outside a seasonal contract'),
    ('ErpMinimumBillAmount', '5.00', 'Bills of an account below this amount are not sent; the power waits for the next run');
