package com.ccb.techfin.model.sxd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccb.techfin.model.sxd.enums.TaskStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sxd_record")
public class ApplicationRecord {

    @TableId(value = "task_id", type = IdType.INPUT)
    private String taskId;

    @TableField("credit_code")
    private String creditCode;

    @TableField("cst_id")
    private String cstId;

    @TableField("status")
    private TaskStatus status;

    /** 实际控制人姓名，用户确认后回填 */
    @TableField("act_cntlr_nm")
    private String actCntlrNm;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
