package com.ccb.techfin.model.sxd.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量新增后返回的文档信息，嵌入 application_doc 表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class DocEntry {

    private String docId;
    private String businessType;
    private String reportDate;
}
