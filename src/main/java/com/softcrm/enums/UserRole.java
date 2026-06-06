package com.softcrm.enums;


public enum UserRole {

    SUPER_ADMIN("Super Admin", "Full system access with all permissions", 100),
    ADMIN("Admin", "Administrative access with user management", 80),
    MANAGER("Manager", "Manager level access with team management", 60),
    SALES_EXECUTIVE("Sales Executive", "Sales team member with lead management", 40),
    SUPPORT_EXECUTIVE("Support Executive", "Customer support team with ticket management", 40),
    HR_EXECUTIVE("HR Executive", "HR team with employee management", 50),
    EMPLOYEE("Employee", "Regular employee with basic access", 30),
    CUSTOMER("Customer", "Customer user with limited access", 10);

    private final String displayName;
    private final String description;
    private final int priority; // Higher priority = more access

    UserRole(String displayName, String description, int priority) {
        this.displayName = displayName;
        this.description = description;
        this.priority = priority;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isAdminRole() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    public boolean isManagementRole() {
        return this == SUPER_ADMIN || this == ADMIN || this == MANAGER;
    }

    public static UserRole fromString(String text) {
        for (UserRole role : UserRole.values()) {
            if (role.name().equalsIgnoreCase(text)) {
                return role;
            }
            if (role.displayName.equalsIgnoreCase(text)) {
                return role;
            }
        }
        return CUSTOMER;
    }
}
