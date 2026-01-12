package com.bullhorn.mockservice.webhook;

/**
 * Types of webhook events that can be subscribed to
 */
public enum WebhookEventType {
    // Candidate events
    CANDIDATE_CREATED("candidate.created"),
    CANDIDATE_UPDATED("candidate.updated"),
    CANDIDATE_DELETED("candidate.deleted"),

    // Consultant events
    CONSULTANT_CREATED("consultant.created"),
    CONSULTANT_UPDATED("consultant.updated"),
    CONSULTANT_DELETED("consultant.deleted"),

    // Job Order events
    JOB_ORDER_CREATED("joborder.created"),
    JOB_ORDER_UPDATED("joborder.updated"),
    JOB_ORDER_DELETED("joborder.deleted"),

    // Job Submission events
    JOB_SUBMISSION_CREATED("jobsubmission.created"),
    JOB_SUBMISSION_UPDATED("jobsubmission.updated"),
    JOB_SUBMISSION_DELETED("jobsubmission.deleted"),

    // Wildcard - subscribe to all events
    ALL("*");

    private final String value;

    WebhookEventType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static WebhookEventType fromValue(String value) {
        for (WebhookEventType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown webhook event type: " + value);
    }
}
