package io.pillopl.dto.pedestallink.v2;

import java.io.Serializable;

/**
 * Link v2 (and v3), answer to vendor data: ACCEPTED, UNKNOWN_VENDOR (the vendor id is not the manufacturer of the
 * pedestal type) or UNKNOWN_MESSAGE_ID; data is empty, the pedestal backend sends nothing back.
 */
public record VendorDataResponse(String status, String data) implements Serializable {

    public static final String ACCEPTED = "ACCEPTED";

    public static final String UNKNOWN_VENDOR = "UNKNOWN_VENDOR";

    public static final String UNKNOWN_MESSAGE_ID = "UNKNOWN_MESSAGE_ID";
}
