package com.bullhorn.mockservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Response DTO for JobOrder
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobOrderResponse {
    private Long id;
    private String bullhornId;
    private String title;
    private String description;
    private String employmentType;
    private Set<String> requiredSkills;
    private Set<String> preferredSkills;
    private String experienceLevel;
    private String location;
    private BigDecimal salary;
    private String clientName;
    private String clientBullhornId;
    private String status;
    private LocalDate openDate;
    private LocalDate closeDate;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
