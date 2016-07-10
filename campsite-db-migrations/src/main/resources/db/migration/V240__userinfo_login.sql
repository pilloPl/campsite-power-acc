-- Console login: PASSWORD_HASH holds a BCrypt hash (or nothing while the user is invited and has not chosen a
-- password). Earlier rows carried a random placeholder; they lose it and have to get a password from an
-- administrator. LAST_LOGIN_DATE and FAILED_LOGINS are maintained by the login.

UPDATE USERINFO SET PASSWORD_HASH = NULL WHERE PASSWORD_HASH IS NOT NULL AND PASSWORD_HASH !~ '^\$2[aby]\$[0-9]{2}\$';

ALTER TABLE USERINFO
    ADD CONSTRAINT CHECK_USERINFO_PASSWORD_HASH CHECK (PASSWORD_HASH IS NULL OR PASSWORD_HASH ~ '^\$2[aby]\$[0-9]{2}\$');

ALTER TABLE USERINFO ADD COLUMN LAST_LOGIN_DATE TIMESTAMP;
ALTER TABLE USERINFO ADD COLUMN FAILED_LOGINS INTEGER NOT NULL DEFAULT 0;
ALTER TABLE USERINFO ADD CONSTRAINT CHECK_USERINFO_FAILED_LOGINS CHECK (FAILED_LOGINS >= 0);

INSERT INTO SYSTEM_PROPERTY (PROPERTY_KEY, PROPERTY_VALUE, DESCRIPTION) VALUES
    ('MaxFailedLogins', '5', 'A console user is locked after this many failed logins in a row');
