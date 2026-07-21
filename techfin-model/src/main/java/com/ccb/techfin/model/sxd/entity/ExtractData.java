package com.ccb.techfin.model.sxd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 提取数据缓存实体，映射 sxd_extract_data 表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sxd_extract_data")
public class ExtractData {

    /** 自增主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联 sxd_record.task_id */
    @TableField("task_id")
    private String taskId;

    /** 关联 sxd_doc.doc_id */
    @TableField("doc_id")
    private String docId;

    /** 提取表名，如 dib_manage_company_profile */
    @TableField("table_name")
    private String tableName;

    /** 提取文本内容 */
    @TableField("text")
    private String text;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}