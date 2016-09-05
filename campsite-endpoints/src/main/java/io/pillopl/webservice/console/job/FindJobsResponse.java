package io.pillopl.webservice.console.job;

import io.pillopl.dto.admin.JobDTO;

import java.io.Serializable;
import java.util.List;

/**
 * The jobs by name.
 */
public record FindJobsResponse(List<JobDTO> jobs) implements Serializable {

    public FindJobsResponse {
        jobs = jobs != null ? List.copyOf(jobs) : List.of();
    }
}
