package com.ccb.techfin.model.sxd.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 文档批量新增返回的文档信息。
 * 外部 API 返回 camelCase 字段名，加 @JsonNaming 覆盖全局 SNAKE_CASE。
 */
@Data
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocInfo {

    private String id;
    private String docName;
    private String projectId;
    private String attId;
}
