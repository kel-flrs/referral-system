package com.bullhorn.mockservice.service.impl;

import com.bullhorn.mockservice.dto.request.CreateJobSubmissionRequest;
import com.bullhorn.mockservice.dto.response.JobSubmissionResponse;
import com.bullhorn.mockservice.entity.Candidate;
import com.bullhorn.mockservice.entity.Consultant;
import com.bullhorn.mockservice.entity.JobOrder;
import com.bullhorn.mockservice.entity.JobSubmission;
import com.bullhorn.mockservice.exception.BullhornApiException;
import com.bullhorn.mockservice.exception.ResourceNotFoundException;
import com.bullhorn.mockservice.repository.CandidateRepository;
import com.bullhorn.mockservice.repository.ConsultantRepository;
import com.bullhorn.mockservice.repository.JobOrderRepository;
import com.bullhorn.mockservice.repository.JobSubmissionRepository;
import com.bullhorn.mockservice.service.JobSubmissionService;
import com.bullhorn.mockservice.service.webhook.WebhookNotificationService;
import com.bullhorn.mockservice.webhook.WebhookEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of JobSubmissionService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobSubmissionServiceImpl implements JobSubmissionService {

    private final JobSubmissionRepository jobSubmissionRepository;
    private final CandidateRepository candidateRepository;
    private final JobOrderRepository jobOrderRepository;
    private final ConsultantRepository consultantRepository;
    private final WebhookNotificationService webhookNotificationService;

    @Override
    @Transactional
    public JobSubmissionResponse createJobSubmission(CreateJobSubmissionRequest request) {
        log.info("Creating job submission for candidate {} to job order {}",
                request.getCandidateId(), request.getJobOrderId());

        // Check if submission already exists
        if (jobSubmissionRepository.existsByCandidateIdAndJobOrderId(
                request.getCandidateId(), request.getJobOrderId())) {
            throw new BullhornApiException("Submission already exists for this candidate and job order");
        }

        Candidate candidate = candidateRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", request.getCandidateId()));

        JobOrder jobOrder = jobOrderRepository.findById(request.getJobOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("JobOrder", "id", request.getJobOrderId()));

        Consultant consultant = consultantRepository.findById(request.getSendingConsultantId())
                .orElseThrow(() -> new ResourceNotFoundException("Consultant", "id", request.getSendingConsultantId()));

        JobSubmission submission = JobSubmission.builder()
                .candidate(candidate)
                .jobOrder(jobOrder)
                .sendingConsultant(consultant)
                .status(request.getStatus())
                .source(request.getSource())
                .comments(request.getComments())
                .build();

        JobSubmission savedSubmission = jobSubmissionRepository.save(submission);
        log.info("Created job submission with ID: {}", savedSubmission.getId());

        JobSubmissionResponse response = JobSubmissionResponse.builder()
                .id(savedSubmission.getId())
                .candidateId(candidate.getId())
                .jobOrderId(jobOrder.getId())
                .sendingConsultantId(consultant.getId())
                .status(savedSubmission.getStatus())
                .source(savedSubmission.getSource())
                .comments(savedSubmission.getComments())
                .createdAt(savedSubmission.getCreatedAt())
                .build();

        // Notify webhook subscribers
        webhookNotificationService.notifySubscribers(WebhookEventType.JOB_SUBMISSION_CREATED, response);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public JobSubmissionResponse getJobSubmissionById(Long id) {
        log.info("Fetching job submission with ID: {}", id);

        JobSubmission submission = jobSubmissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobSubmission", "id", id));

        return JobSubmissionResponse.builder()
                .id(submission.getId())
                .candidateId(submission.getCandidate().getId())
                .jobOrderId(submission.getJobOrder().getId())
                .sendingConsultantId(submission.getSendingConsultant().getId())
                .status(submission.getStatus())
                .source(submission.getSource())
                .comments(submission.getComments())
                .createdAt(submission.getCreatedAt())
                .build();
    }
}
