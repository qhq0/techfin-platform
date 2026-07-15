package com.ccb.techfin.model.sxd.dto.external;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提取数据查询外部 API 请求体。
 * POST /api/extract/open/doc/queryData
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class ExtractQueryDataRequest {

    /** 文档 ID（来自 application_doc.doc_id） */
    private Long docId;

    /** 表名，对应商业计划书提取的不同部分 */
    private String tableName;
}
