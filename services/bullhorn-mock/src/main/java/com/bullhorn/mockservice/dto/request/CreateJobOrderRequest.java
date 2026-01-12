package com.bullhorn.mockservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * Request DTO for creating a JobOrder
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateJobOrderRequest {

    @NotBlank(message = "Title is required")
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
}
