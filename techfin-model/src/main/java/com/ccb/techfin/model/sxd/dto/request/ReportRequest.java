package com.ccb.techfin.model.sxd.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class ReportRequest {

    /** 任务 ID */
    private String taskId;

    /** 客户编号 */
    private String cstId;
}