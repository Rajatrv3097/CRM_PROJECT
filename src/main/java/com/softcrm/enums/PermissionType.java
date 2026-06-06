package com.softcrm.enums;


public enum PermissionType {

    CREATE("Create", "Permission to create new records"),
    READ("Read", "Permission to view records"),
    UPDATE("Update", "Permission to modify records"),
    DELETE("Delete", "Permission to delete records"),
    EXPORT("Export", "Permission to export data"),
    APPROVE("Approve", "Permission to approve requests"),
    ASSIGN("Assign", "Permission to assign tasks/leads");

    private final String action;
    private final String description;

    PermissionType(String action, String description) {
        this.action = action;
        this.description = description;
    }

    public String getAction() {
        return action;
    }

    public String getDescription() {
        return description;
    }

    public String getPermissionName(String resource) {
        return resource + "_" + this.name();
    }

    public static PermissionType fromAction(String action) {
        for (PermissionType type : PermissionType.values()) {
            if (type.action.equalsIgnoreCase(action)) {
                return type;
            }
            if (type.name().equalsIgnoreCase(action)) {
                return type;
            }
        }
        return READ;
    }
}
