package io.pillopl.dto.pedestallink.v1;

import java.io.Serializable;

/**
 * Link v1: progress of a firmware update (DOWNLOADED, DOWNLOAD_FAILED, INSTALLED, INSTALLATION_FAILED).
 */
public record FirmwareStatusRequest(String status) implements Serializable {
}
