-- RFID_UTILS: conversions of card UIDs. Issuers deliver UIDs as big-endian hex ("04A1B2C3D4"), the pedestal readers
-- send the decimal value of the bytes in reader order. Same rules as io.pillopl.card.RfidConverter.

CREATE SCHEMA RFID_UTILS;

CREATE FUNCTION RFID_UTILS.HEX_TO_DEC(p_hex TEXT) RETURNS NUMERIC
    LANGUAGE plpgsql IMMUTABLE STRICT AS
$$
DECLARE
    v_result NUMERIC := 0;
    v_digit  INTEGER;
    v_char   TEXT;
BEGIN
    FOR i IN 1 .. length(p_hex) LOOP
        v_char := upper(substr(p_hex, i, 1));
        v_digit := position(v_char IN '0123456789ABCDEF') - 1;
        IF v_digit < 0 THEN
            RAISE EXCEPTION 'Not a hex digit: % in %', v_char, p_hex;
        END IF;
        v_result := v_result * 16 + v_digit;
    END LOOP;
    RETURN v_result;
END;
$$;

CREATE FUNCTION RFID_UTILS.DEC_TO_HEX(p_dec NUMERIC) RETURNS TEXT
    LANGUAGE plpgsql IMMUTABLE STRICT AS
$$
DECLARE
    v_rest   NUMERIC := trunc(p_dec);
    v_result TEXT := '';
BEGIN
    IF v_rest < 0 THEN
        RAISE EXCEPTION 'Negative UID: %', p_dec;
    END IF;
    IF v_rest = 0 THEN
        RETURN '0';
    END IF;
    WHILE v_rest > 0 LOOP
        v_result := substr('0123456789ABCDEF', (mod(v_rest, 16))::INTEGER + 1, 1) || v_result;
        v_rest := div(v_rest, 16);
    END LOOP;
    RETURN v_result;
END;
$$;

-- reverses the order of the bytes (pairs of hex digits); an odd number of digits gets a leading zero
CREATE FUNCTION RFID_UTILS.TURN_BYTE_ORDER(p_hex TEXT) RETURNS TEXT
    LANGUAGE plpgsql IMMUTABLE STRICT AS
$$
DECLARE
    v_hex    TEXT := upper(p_hex);
    v_result TEXT := '';
BEGIN
    IF mod(length(v_hex), 2) = 1 THEN
        v_hex := '0' || v_hex;
    END IF;
    FOR i IN REVERSE (length(v_hex) / 2) .. 1 LOOP
        v_result := v_result || substr(v_hex, i * 2 - 1, 2);
    END LOOP;
    RETURN v_result;
END;
$$;

CREATE FUNCTION RFID_UTILS.DEC_TO_HEX_BIGENDIAN(p_dec NUMERIC) RETURNS TEXT
    LANGUAGE plpgsql IMMUTABLE STRICT AS
$$
BEGIN
    RETURN RFID_UTILS.TURN_BYTE_ORDER(RFID_UTILS.DEC_TO_HEX(p_dec));
END;
$$;

CREATE FUNCTION RFID_UTILS.DEC_TO_HEX_BIGENDIAN(p_dec TEXT) RETURNS TEXT
    LANGUAGE plpgsql IMMUTABLE STRICT AS
$$
BEGIN
    RETURN RFID_UTILS.DEC_TO_HEX_BIGENDIAN(p_dec::NUMERIC);
END;
$$;

CREATE FUNCTION RFID_UTILS.HEX_BIGENDIAN_TO_DEC(p_hex TEXT) RETURNS NUMERIC
    LANGUAGE plpgsql IMMUTABLE STRICT AS
$$
BEGIN
    RETURN RFID_UTILS.HEX_TO_DEC(RFID_UTILS.TURN_BYTE_ORDER(p_hex));
END;
$$;
