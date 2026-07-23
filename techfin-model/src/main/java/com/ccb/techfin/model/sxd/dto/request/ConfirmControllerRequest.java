package com.ccb.techfin.model.sxd.dto.request;

import lombok.Data;

/**
 * 确认/修改实控人姓名请求体。
 *
 * @author qiuhaoquan
 * @since 2026-07-23
 */
@Data
public class ConfirmControllerRequest {

    /** 任务 ID，对应 sxd_record.task_id */
    private String taskId;

    /** 实际控制人姓名（用户确认或修改后的值） */
    private String actCntlrNm;
}