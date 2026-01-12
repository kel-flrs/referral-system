package com.bullhorn.mockservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;
import java.util.Set;

/**
 * Request DTO for creating a Candidate
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCandidateRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
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
