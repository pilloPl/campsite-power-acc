-- Seed: the roots per market and the named groups the code looks up (ids below the sequence start).
INSERT INTO TBL_GROUP (ID, PARENT_ID, NAME, TYPENAME, DISPLAYNAME) VALUES
    (1, NULL, 'HR ISSUERS', 'ISSUER', 'Croatia card issuers'),
    (2, NULL, 'IT ISSUERS', 'ISSUER', 'Italy card issuers'),
    (3, NULL, 'FR ISSUERS', 'ISSUER', 'France card issuers'),
    (11, NULL, 'HR SITES', 'SITE', 'Croatia campsites'),
    (12, NULL, 'IT SITES', 'SITE', 'Italy campsites'),
    (13, NULL, 'FR SITES', 'SITE', 'France campsites'),
    (21, NULL, 'HR STAFF', 'STAFF', 'Croatia staff'),
    (22, NULL, 'IT STAFF', 'STAFF', 'Italy staff'),
    (23, NULL, 'FR STAFF', 'STAFF', 'France staff'),
    -- the network's own card (home market HR)
    (31, 1, 'NETWORK CARD', 'ISSUER', 'Network card'),
    -- welcome cards shipped with an affiliate kit, waiting to be adopted by an affiliate campsite
    (41, 1, 'HR WELCOME_CARD_POOL', 'ISSUER', 'Croatia welcome card pool'),
    (42, 2, 'IT WELCOME_CARD_POOL', 'ISSUER', 'Italy welcome card pool'),
    (43, 3, 'FR WELCOME_CARD_POOL', 'ISSUER', 'France welcome card pool'),
    -- parents of affiliate campsites: pedestals open to network cards vs. house cards only
    (51, 11, 'NETWORK ACCESS HR', 'SITE', 'Network access Croatia'),
    (52, 12, 'NETWORK ACCESS IT', 'SITE', 'Network access Italy'),
    (53, 13, 'NETWORK ACCESS FR', 'SITE', 'Network access France'),
    (61, 11, 'HOUSE CARDS ONLY HR', 'SITE', 'House cards only Croatia'),
    (62, 12, 'HOUSE CARDS ONLY IT', 'SITE', 'House cards only Italy'),
    (63, 13, 'HOUSE CARDS ONLY FR', 'SITE', 'House cards only France'),
    -- reception desks
    (71, 21, 'HR RECEPTION', 'STAFF', 'Croatia reception'),
    (72, 22, 'IT RECEPTION', 'STAFF', 'Italy reception'),
    (73, 23, 'FR RECEPTION', 'STAFF', 'France reception');
