package io.pillopl.dto.pedestallink.v1;

import java.io.Serializable;

/**
 * Link v1: the controller of the pedestal started and describes itself.
 */
public record BootRequest(String vendor, String model, String serialNumber, String firmwareVersion,
                          String modemIccid, String meterType, String meterSerialNumber) implements Serializable {
}
