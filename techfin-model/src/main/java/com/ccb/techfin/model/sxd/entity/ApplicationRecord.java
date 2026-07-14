package com.ccb.techfin.model.sxd.entity;

import com.ccb.techfin.model.sxd.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "application_record")
public class ApplicationRecord {

    @Id
    @Column(name = "task_id", length = 64)
    private String taskId;

    @Column(name = "credit_code", nullable = false, length = 18)
    private String creditCode;

    @Column(name = "customer_no", length = 64)
    private String customerNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TaskStatus status;

    @ElementCollection
    @CollectionTable(name = "application_doc", joinColumns = @JoinColumn(name = "task_id"),
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private List<DocEntry> docEntries = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
