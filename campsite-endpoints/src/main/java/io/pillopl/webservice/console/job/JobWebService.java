package io.pillopl.webservice.console.job;

import io.pillopl.api.JobApi;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web service of the jobs in the administration tab: list, run now, pause and resume.
 */
@RestController
@RequestMapping(WebServicePaths.JOB)
public class JobWebService extends AbstractWebService<JobApi, JobWebServiceFault> {

    public JobWebService(JobApi jobApi) {
        super(jobApi);
    }

    @PostMapping("/findJobs")
    public FindJobsResponse findJobs(@RequestBody FindJobsRequest request) throws JobWebServiceFault {
        return invoke("findJobs", () -> new FindJobsResponse(getBean().findJobs(caller())));
    }

    @PostMapping("/runJobNow")
    public JobActionResponse runJobNow(@RequestBody JobActionRequest request) throws JobWebServiceFault {
        return invoke("runJobNow", () -> new JobActionResponse(getBean().runJobNow(caller(), request.jobName())));
    }

    @PostMapping("/pauseJob")
    public JobActionResponse pauseJob(@RequestBody JobActionRequest request) throws JobWebServiceFault {
        return invoke("pauseJob", () -> new JobActionResponse(getBean().pauseJob(caller(), request.jobName())));
    }

    @PostMapping("/resumeJob")
    public JobActionResponse resumeJob(@RequestBody JobActionRequest request) throws JobWebServiceFault {
        return invoke("resumeJob", () -> new JobActionResponse(getBean().resumeJob(caller(), request.jobName())));
    }

    @Override
    protected JobWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new JobWebServiceFault(message, errorCode);
    }
}
