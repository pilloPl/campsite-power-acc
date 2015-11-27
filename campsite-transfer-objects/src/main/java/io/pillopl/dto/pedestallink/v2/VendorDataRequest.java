package io.pillopl.dto.pedestallink.v2;

import java.io.Serializable;

/**
 * Link v2 (and v3): free data of the controller's vendor (door contact, housing temperature, display texts), which
 * the link itself does not define. The message id tells the vendor's kind of data.
 */
public record VendorDataRequest(String vendorId, String messageId, String data) implements Serializable {
}
