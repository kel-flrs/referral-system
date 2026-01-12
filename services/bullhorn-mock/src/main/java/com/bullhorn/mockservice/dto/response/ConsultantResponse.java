package com.bullhorn.mockservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO for Consultant
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsultantResponse {
    private Long id;
    private String bullhornId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Boolean isActive;
    private LocalDateTime lastActivityAt;
    private Integer totalPlacements;
    private Integer totalReferrals;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
