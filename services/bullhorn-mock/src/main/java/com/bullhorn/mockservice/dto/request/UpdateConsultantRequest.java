package com.bullhorn.mockservice.dto.request;

import jakarta.validation.constraints.Email;
import lombok.*;

/**
 * Request DTO for updating a Consultant
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateConsultantRequest {

    private String firstName;
    private String lastName;

    @Email(message = "Email must be valid")
    private String email;

    private String phone;
    private Boolean isActive;
}
