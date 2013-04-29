-- Short code of the campsite (e.g. HRBAY): prefix of the socket codes and of the consumption record ids.
ALTER TABLE LOCATION ADD COLUMN CAMPSITE_CODE VARCHAR(16);
