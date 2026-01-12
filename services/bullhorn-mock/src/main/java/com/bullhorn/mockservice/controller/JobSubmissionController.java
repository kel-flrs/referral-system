package com.bullhorn.mockservice.controller;

import com.bullhorn.mockservice.dto.request.CreateJobSubmissionRequest;
import com.bullhorn.mockservice.dto.response.ApiResponse;
import com.bullhorn.mockservice.dto.response.JobSubmissionResponse;
import com.bullhorn.mockservice.service.JobSubmissionService;
import com.bullhorn.mockservice.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for JobSubmission operations
 */
@Slf4j
@RestController
@RequestMapping(Constants.API_VERSION + "/submissions")
@RequiredArgsConstructor
@Tag(name = "Job Submissions", description = "Job submission management APIs")
public class JobSubmissionController {

    private final JobSubmissionService jobSubmissionService;

    @PostMapping
    @Operation(summary = "Create a new job submission (candidate referral)")
    public ResponseEntity<ApiResponse<JobSubmissionResponse>> createJobSubmission(
            @Valid @RequestBody CreateJobSubmissionRequest request
    ) {
        JobSubmissionResponse submission = jobSubmissionService.createJobSubmission(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(submission, "Job submission created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job submission by ID")
    public ResponseEntity<ApiResponse<JobSubmissionResponse>> getJobSubmissionById(@PathVariable Long id) {
        JobSubmissionResponse submission = jobSubmissionService.getJobSubmissionById(id);
        return ResponseEntity.ok(ApiResponse.success(submission));
    }
}
