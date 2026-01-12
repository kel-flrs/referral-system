package com.bullhorn.mockservice.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response format
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private boolean success;
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<ValidationError> validationErrors;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationError {
        private String field;
        private String message;
    }
}
