package com.bullhorn.mockservice.dto.response;

import lombok.*;

import java.time.LocalDate;

/**
 * DTO for Experience response
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExperienceDto {
    private Long id;
    private String companyName;
    private String jobTitle;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
}
