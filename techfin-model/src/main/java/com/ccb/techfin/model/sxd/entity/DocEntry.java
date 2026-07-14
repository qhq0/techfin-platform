package com.ccb.techfin.model.sxd.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量新增后返回的文档信息，独立实体映射 application_doc 表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("application_doc")
public class DocEntry {

    /** 资料批量新增返回的文档 ID（全局唯一，用作主键） */
    @TableId("doc_id")
    private String docId;

    /** 关联的申请记录 taskId */
    @TableField("task_id")
    private String taskId;

    /** 业务类型（finance / business），从请求体回填 */
    @TableField("business_type")
    private String businessType;

    /** 该文档对应的财报报告日期，仅财务报表有效 */
    @TableField("report_date")
    private String reportDate;
}
