package com.ccb.techfin.sxd.enums;

public enum TaskStatus {
    PENDING_ANALYSIS("Step 1 Completed"),
    ANALYSIS_IN_PROGRESS("Analysis In Progress"),
    COMPLETED("Completed"),
    REJECTED("Rejected");

    private final String description;

    TaskStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
