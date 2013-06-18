package io.pillopl.webservice.console.group;

import java.io.Serializable;

/**
 * Moves an affiliate campsite to network access or house cards only.
 */
public record SwitchAccessModeRequest(String authUsername, Long affiliateGroupId, String accessMode,
                                      Boolean listedInApp)
        implements Serializable {
}
