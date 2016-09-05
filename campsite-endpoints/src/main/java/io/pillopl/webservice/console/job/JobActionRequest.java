package io.pillopl.webservice.console.job;

import java.io.Serializable;

/**
 * The job to run now, pause or resume (name as listed, e.g. eventRetryJob).
 */
public record JobActionRequest(String jobName) implements Serializable {
}
