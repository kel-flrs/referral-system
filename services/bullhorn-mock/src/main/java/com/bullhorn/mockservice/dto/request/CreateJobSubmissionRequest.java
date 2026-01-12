package com.bullhorn.mockservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for creating a JobSubmission
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateJobSubmissionRequest {

    @NotNull(message = "Candidate ID is required")
    private Long candidateId;

    @NotNull(message = "Job Order ID is required")
    private Long jobOrderId;

    @NotNull(message = "Sending Consultant ID is required")
    private Long sendingConsultantId;

    private String status;
    private String source;
    private String comments;
}
