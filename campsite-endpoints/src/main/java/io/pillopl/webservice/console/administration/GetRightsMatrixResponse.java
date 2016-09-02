package io.pillopl.webservice.console.administration;

import io.pillopl.dto.admin.RightsMatrixDTO;

import java.io.Serializable;

/**
 * All rights, the roles with their rights and the security groups with their roles.
 */
public record GetRightsMatrixResponse(RightsMatrixDTO rightsMatrix) implements Serializable {
}
