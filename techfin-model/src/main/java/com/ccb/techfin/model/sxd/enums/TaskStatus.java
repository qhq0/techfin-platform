package com.ccb.techfin.model.sxd.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum TaskStatus implements IEnum<String> {
    DRAFT("Draft - Attachments Uploaded"),
    PENDING_ANALYSIS("Pending Analysis"),
    ANALYSIS_IN_PROGRESS("Analysis In Progress"),
    COMPLETED("Completed"),
    REJECTED("Rejected");

    private final String description;

    TaskStatus(String description) {
        this.description = description;
    }

    @Override
    public String getValue() {
        return name();
    }

    public String getDescription() {
        return description;
    }
}
