-- ORDER_UTILS.UPDATE_ERP_NUMBERS: the ERP corrected the partner and account number of a guest. The orders with the
-- old numbers get the new ones; for their guests the billing account, the guest's partner number and the cards
-- billed to the old account follow. No order with the old numbers: nothing changes, 0 is returned.
--   SELECT ORDER_UTILS.UPDATE_ERP_NUMBERS(old_partner, old_account, new_partner, new_account);

CREATE SCHEMA ORDER_UTILS;

CREATE FUNCTION ORDER_UTILS.UPDATE_ERP_NUMBERS(p_partner_no_old TEXT, p_account_no_old TEXT, p_partner_no_new TEXT,
                                               p_account_no_new TEXT) RETURNS INTEGER AS
$$
DECLARE
    v_order    RECORD;
    v_updated  INTEGER := 0;
BEGIN
    IF p_partner_no_new IS NULL OR p_account_no_new IS NULL THEN
        RAISE EXCEPTION 'New ERP partner and account number are both needed';
    END IF;

    FOR v_order IN
        UPDATE ORDER_HEADER
           SET ERP_PARTNER_NO = p_partner_no_new,
               ERP_ACCOUNT_NO = p_account_no_new,
               MOD_DATE       = now()
         WHERE GUEST_ID IS NOT NULL
           AND ERP_PARTNER_NO = p_partner_no_old
           AND ERP_ACCOUNT_NO = p_account_no_old
        RETURNING ID, GUEST_ID
    LOOP
        v_updated := v_updated + 1;

        UPDATE BILLING_ACCOUNT
           SET ERP_ACCOUNT_NO = p_account_no_new
         WHERE GUEST_ID = v_order.GUEST_ID
           AND ERP_ACCOUNT_NO = p_account_no_old;

        UPDATE GUEST
           SET ERP_PARTNER_NO = p_partner_no_new,
               MOD_DATE       = now()
         WHERE ID = v_order.GUEST_ID
           AND ERP_PARTNER_NO = p_partner_no_old;

        UPDATE CAMPING_CARD
           SET BILLING_ACCOUNT_NO = p_account_no_new
         WHERE GUEST_ID = v_order.GUEST_ID
           AND BILLING_ACCOUNT_NO = p_account_no_old
           AND DELETED = FALSE;
    END LOOP;

    RETURN v_updated;
END;
$$ LANGUAGE plpgsql;
