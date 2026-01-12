package com.bullhorn.mockservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Response DTO for Candidate
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CandidateResponse {
    private Long id;
    private String bullhornId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String currentTitle;
    private String currentCompany;
    private String location;
    private Set<String> skills;
    private String summary;
    private List<ExperienceDto> experience;
    private List<EducationDto> education;
    private String status;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
