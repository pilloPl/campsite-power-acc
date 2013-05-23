package io.pillopl.dto.group;

import java.io.Serializable;
import java.util.List;

/**
 * Where the cards of an ISSUER group are accepted: the agreements in force (own or inherited from the nearest
 * ancestor with a non-empty list) and all SITE group ids below them.
 */
public record AcceptanceAgreementDTO(Long issuerGroupId, boolean inherited, List<GroupDTO> agreements,
                                     List<Long> acceptingSiteGroupIds) implements Serializable {
}
