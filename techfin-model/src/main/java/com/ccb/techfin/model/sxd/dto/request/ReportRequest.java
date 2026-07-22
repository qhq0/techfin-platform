package com.ccb.techfin.model.sxd.dto.request;

import lombok.Data;

@Data
public class ReportRequest {

    /** 任务 ID */
    private String taskId;

    /** 客户编号 */
    private String cstId;
}