package com.ccb.techfin.model.sxd.enums;

public enum TaskStatus {
    DRAFT("Draft - Attachments Uploaded"),
    PENDING_ANALYSIS("Pending Analysis"),
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
