package com.ccb.techfin.model.sxd.dto.external;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 资料批量新增请求参数。
 * 外部 API 要求 camelCase 字段名，加 @JsonNaming 覆盖全局 SNAKE_CASE。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class DocBatchAddItem {

    private String attId;
    private Long dirId;
    private String docName;
    private Long docSize;
    private Long docTypeId;
    private String extraInfo;
    private Long projectId;
    private String reportDate;
}
