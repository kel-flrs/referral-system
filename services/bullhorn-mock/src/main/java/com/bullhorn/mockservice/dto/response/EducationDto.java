package com.bullhorn.mockservice.dto.response;

import lombok.*;

import java.time.LocalDate;

/**
 * DTO for Education response
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EducationDto {
    private Long id;
    private String school;
    private String degree;
    private LocalDate startDate;
    private LocalDate endDate;
    private String fieldOfStudy;
}
