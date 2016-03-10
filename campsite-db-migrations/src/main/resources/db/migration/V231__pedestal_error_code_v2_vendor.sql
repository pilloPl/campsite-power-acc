-- Vendor error codes of the link v2 controllers of ADRIATEC pedestals. A v2 socket status carries the vendor code next
-- to the link error code; the vendor code is translated with these manufacturer rows first, the link code with the
-- manufacturer rows, then the link rows (MANUFACTURER = 'DEFAULT'). Inserted with nextval, the ids stay free.

INSERT INTO PEDESTAL_ERROR_CODE (ID, MANUFACTURER, CODE, MASK, MASKED, ABBREVIATION, DESCRIPTION) VALUES
    (nextval('PEDESTAL_ERROR_CODE_ID_SEQ'), 'ADRIATEC', 'E00', 0, FALSE, 'OK', 'No error'),
    (nextval('PEDESTAL_ERROR_CODE_ID_SEQ'), 'ADRIATEC', 'E01', 1, TRUE, 'BRK', 'Miniature circuit breaker open'),
    (nextval('PEDESTAL_ERROR_CODE_ID_SEQ'), 'ADRIATEC', 'E02', 2, TRUE, 'RCD', 'Residual current device open'),
    (nextval('PEDESTAL_ERROR_CODE_ID_SEQ'), 'ADRIATEC', 'E07', 4, TRUE, 'TMP', 'Housing temperature above 70 C'),
    (nextval('PEDESTAL_ERROR_CODE_ID_SEQ'), 'ADRIATEC', 'E10', 8, TRUE, 'MTR', 'No answer of the socket meter'),
    (nextval('PEDESTAL_ERROR_CODE_ID_SEQ'), 'ADRIATEC', 'E21', 64, TRUE, 'WTR', 'Moisture sensor in the socket housing'),
    (nextval('PEDESTAL_ERROR_CODE_ID_SEQ'), 'ADRIATEC', 'E30', 16, TRUE, 'RDR', 'Card reader does not answer'),
    (nextval('PEDESTAL_ERROR_CODE_ID_SEQ'), 'ADRIATEC', 'E31', 32, TRUE, 'MDM', 'Modem without network'),
    (nextval('PEDESTAL_ERROR_CODE_ID_SEQ'), 'ADRIATEC', 'E99', 128, TRUE, 'OTH', 'Other controller error'),
    -- the error flags of an ADRIATEC pedestal are read back with its own rows only: it needs the unknown error too
    (nextval('PEDESTAL_ERROR_CODE_ID_SEQ'), 'ADRIATEC', 'UNKNOWN_ERROR', 4611686018427387904, FALSE, 'UNK',
     'Unknown error code');
