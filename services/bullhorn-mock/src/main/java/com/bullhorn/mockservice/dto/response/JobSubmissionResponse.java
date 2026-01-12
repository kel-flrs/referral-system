package com.bullhorn.mockservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO for JobSubmission
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobSubmissionResponse {
    private Long id;
    private Long candidateId;
    private Long jobOrderId;
    private Long sendingConsultantId;
    private String status;
    private String source;
    private String comments;
    private LocalDateTime createdAt;
}
