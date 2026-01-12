package com.bullhorn.mockservice.dto.request;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * Request DTO for updating a JobOrder
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateJobOrderRequest {

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
