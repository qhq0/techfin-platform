package com.ccb.techfin.model.sxd.dto.request;

import lombok.Data;

/**
 * 报告生成请求体。
 *
 * @author qiuhaoquan
 * @since 2026-07-23
 */
@Data
public class ReportRequest {

    /** 任务 ID */
    private String taskId;

    /** 客户编号 */
    private String cstId;
}