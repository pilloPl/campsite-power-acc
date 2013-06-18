package io.pillopl.webservice.console.group;

import java.io.Serializable;

/**
 * Access mode of the affiliate campsite after the switch.
 */
public record SwitchAccessModeResponse(Long affiliateGroupId, String accessMode) implements Serializable {
}
