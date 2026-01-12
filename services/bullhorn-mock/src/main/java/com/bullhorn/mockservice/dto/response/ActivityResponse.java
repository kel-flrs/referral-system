package com.bullhorn.mockservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO for Activity
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityResponse {
    private Long id;
    private String action;
    private ConsultantResponse consultant;
    private String comments;
    private LocalDateTime activityDate;
    private LocalDateTime createdAt;
}
