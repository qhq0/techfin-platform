package com.ccb.techfin.sxd.entity;

import com.ccb.techfin.sxd.enums.TaskStatus;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false, unique = true, length = 64)
    private String taskId;

    @Column(name = "credit_code", nullable = false, length = 18)
    private String creditCode;

    @Column(name = "customer_no", nullable = false, length = 64)
    private String customerNo;

    @Column(name = "report_date", length = 10)
    private String reportDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TaskStatus status;

    @ElementCollection
    @CollectionTable(name = "application_doc", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "doc_id", length = 64)
    private List<String> docIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "application_att", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "att_id", length = 64)
    private List<String> attIds = new ArrayList<>();

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
