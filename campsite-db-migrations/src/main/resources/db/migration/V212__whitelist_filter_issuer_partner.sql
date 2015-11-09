-- The issuer partner a whitelist filter targets: a filter with a partner applies to the exchange with that partner
-- only, a filter without one to the exchange with every partner.

ALTER TABLE GRP_ATTR_WHITELIST_FILTER
    ADD COLUMN ISSUER_PARTNER_ID BIGINT,
    ADD CONSTRAINT FK_WHITELIST_FILTER_ISSUER_PARTNER FOREIGN KEY (ISSUER_PARTNER_ID) REFERENCES ISSUER_PARTNER (ID);
