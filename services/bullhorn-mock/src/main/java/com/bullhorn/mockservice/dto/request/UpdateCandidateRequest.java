package com.bullhorn.mockservice.dto.request;

import jakarta.validation.constraints.Email;
import lombok.*;

import java.util.Set;

/**
 * Request DTO for updating a Candidate
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCandidateRequest {

    private String firstName;
    private String lastName;

    @Email(message = "Email must be valid")
    private String email;

    private String phone;
    private String currentTitle;
    private String currentCompany;
    private String location;
    private Set<String> skills;
    private String summary;
    private String status;
}
