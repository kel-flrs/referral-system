package com.bullhorn.mockservice.util;

/**
 * Application-wide constants
 */
public final class Constants {

    private Constants() {
        // Prevent instantiation
    }

    // API Version
    public static final String API_VERSION = "/api/v1";

    // Entity Status
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_CLOSED = "CLOSED";

    // Employment Types
    public static final String EMPLOYMENT_FULL_TIME = "FULL_TIME";
    public static final String EMPLOYMENT_PART_TIME = "PART_TIME";
    public static final String EMPLOYMENT_CONTRACT = "CONTRACT";
    public static final String EMPLOYMENT_TEMPORARY = "TEMPORARY";

    // Experience Levels
    public static final String LEVEL_ENTRY = "ENTRY";
    public static final String LEVEL_JUNIOR = "JUNIOR";
    public static final String LEVEL_MID = "MID";
    public static final String LEVEL_SENIOR = "SENIOR";
    public static final String LEVEL_LEAD = "LEAD";
    public static final String LEVEL_PRINCIPAL = "PRINCIPAL";

    // Activity Types
    public static final String ACTIVITY_NOTE = "NOTE";
    public static final String ACTIVITY_EMAIL = "EMAIL";
    public static final String ACTIVITY_CALL = "CALL";
    public static final String ACTIVITY_MEETING = "MEETING";

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 1000;  // Increased for bulk retrieval efficiency

    // Error Messages
    public static final String ERROR_RESOURCE_NOT_FOUND = "Resource not found";
    public static final String ERROR_INVALID_REQUEST = "Invalid request";
    public static final String ERROR_INTERNAL_SERVER = "Internal server error";
}
