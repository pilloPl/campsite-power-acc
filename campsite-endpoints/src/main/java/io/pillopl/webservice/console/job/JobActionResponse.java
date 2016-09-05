package io.pillopl.webservice.console.job;

import io.pillopl.dto.admin.JobDTO;

import java.io.Serializable;

/**
 * The job after the action.
 */
public record JobActionResponse(JobDTO job) implements Serializable {
}
