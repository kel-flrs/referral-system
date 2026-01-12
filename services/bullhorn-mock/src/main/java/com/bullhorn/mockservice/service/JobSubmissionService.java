package com.bullhorn.mockservice.service;

import com.bullhorn.mockservice.dto.request.CreateJobSubmissionRequest;
import com.bullhorn.mockservice.dto.response.JobSubmissionResponse;

/**
 * Service interface for JobSubmission operations
 */
public interface JobSubmissionService {

    JobSubmissionResponse createJobSubmission(CreateJobSubmissionRequest request);

    JobSubmissionResponse getJobSubmissionById(Long id);
}
