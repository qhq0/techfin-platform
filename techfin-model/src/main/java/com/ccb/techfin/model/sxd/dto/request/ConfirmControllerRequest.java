package com.ccb.techfin.model.sxd.dto.request;

import lombok.Data;

@Data
public class ConfirmControllerRequest {

    /** 任务 ID，对应 application_record.task_id */
    private String taskId;

    /** 实际控制人姓名（用户确认或修改后的值） */
    private String actCntlrNm;
}
