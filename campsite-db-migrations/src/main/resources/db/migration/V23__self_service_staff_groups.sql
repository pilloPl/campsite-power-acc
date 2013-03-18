-- Self-service console users (affiliate owners, seasonal residents) hang in "<MKT> SELF SERVICE" under the STAFF root.
INSERT INTO TBL_GROUP (ID, PARENT_ID, NAME, TYPENAME, DISPLAYNAME) VALUES
    (81, 21, 'HR SELF SERVICE', 'STAFF', 'Croatia self-service users'),
    (82, 22, 'IT SELF SERVICE', 'STAFF', 'Italy self-service users'),
    (83, 23, 'FR SELF SERVICE', 'STAFF', 'France self-service users');
