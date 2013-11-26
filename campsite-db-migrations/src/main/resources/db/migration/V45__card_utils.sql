-- CARD_UTILS: upsert of a camping card from an issuer file. The UID comes as big-endian hex and is stored as the
-- decimal UID of the readers (RFID_UTILS). The living card with the same UID and number is updated, otherwise a new
-- card is inserted. The guest is passed by id (no lookups by name). Returns the id of the card.

CREATE SCHEMA CARD_UTILS;

CREATE FUNCTION CARD_UTILS.UPSERT_CARD(p_card_rfid_hex TEXT, p_card_number TEXT, p_valid_from DATE,
                                       p_valid_to DATE, p_billing_account_no TEXT, p_guest_id BIGINT)
    RETURNS BIGINT
    LANGUAGE plpgsql AS
$$
DECLARE
    v_card_rfid TEXT := RFID_UTILS.HEX_BIGENDIAN_TO_DEC(p_card_rfid_hex)::TEXT;
    v_card_id   BIGINT;
BEGIN
    UPDATE CAMPING_CARD
       SET STATUS                    = 'ACTIVATED',
           GUEST_ID                  = p_guest_id,
           VALID_FROM                = p_valid_from,
           VALID_TO                  = p_valid_to,
           BILLING_ACCOUNT_NO        = p_billing_account_no,
           CREATE_DATE               = now(),
           UPLOAD_DATE               = NULL,
           PARTNER_SYNC_STATE        = 'UNKNOWN',
           TARGET_PARTNER_SYNC_STATE = NULL
     WHERE CARD_RFID = v_card_rfid
       AND CARD_NUMBER = p_card_number
       AND DELETED = FALSE
    RETURNING ID INTO v_card_id;

    IF v_card_id IS NULL THEN
        INSERT INTO CAMPING_CARD (ID, CARD_RFID, CARD_NUMBER, STATUS, GUEST_ID, VALID_FROM, VALID_TO,
                                  BILLING_ACCOUNT_NO, DELETED, CREATE_DATE, UPLOAD_DATE, PARTNER_SYNC_STATE,
                                  TARGET_PARTNER_SYNC_STATE)
        VALUES (nextval('CAMPING_CARD_ID_SEQ'), v_card_rfid, p_card_number, 'ACTIVATED', p_guest_id, p_valid_from,
                p_valid_to, p_billing_account_no, FALSE, now(), NULL, 'UNKNOWN', NULL)
        RETURNING ID INTO v_card_id;
    END IF;

    RETURN v_card_id;
END;
$$;
