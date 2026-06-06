package com.softcrm.enums;


public enum UserStatus {

    ACTIVE("Active", "User account is active and can login"),
    INACTIVE("Inactive", "User account is inactive"),
    SUSPENDED("Suspended", "User account is temporarily suspended"),
    PENDING_VERIFICATION("Pending Verification", "Email/Phone verification pending"),
    LOCKED("Locked", "User account is locked due to multiple failed attempts"),
    DELETED("Deleted", "User account is soft deleted");

    private final String displayName;
    private final String description;

    UserStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean canLogin() {
        return this == ACTIVE || this == PENDING_VERIFICATION;
    }

    public static UserStatus fromDisplayName(String displayName) {
        for (UserStatus status : UserStatus.values()) {
            if (status.displayName.equalsIgnoreCase(displayName)) {
                return status;
            }
        }
        return INACTIVE;
    }
}
