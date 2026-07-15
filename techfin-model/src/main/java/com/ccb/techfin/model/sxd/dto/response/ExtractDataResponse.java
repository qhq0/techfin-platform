package com.ccb.techfin.model.sxd.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 提取数据查询响应体。
 */
@Data
@AllArgsConstructor
public class ExtractDataResponse {

    /** 提取数据列表，每个元素为一个 tableName 对应的文本 */
    private List<ExtractDataItem> extractData;

    @Data
    @AllArgsConstructor
    public static class ExtractDataItem {

        /** 表名，如 dib_manage_company_profile */
        private String tableName;

        /** 提取的文本内容 */
        private String text;
    }
}
