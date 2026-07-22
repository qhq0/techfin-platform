package com.ccb.techfin.model.sxd.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 提取数据项，对应商业计划书某个 tableName 的提取文本。
 */
@Data
@AllArgsConstructor
public class ExtractDataItem {

    /** 表名，如 dib_manage_company_profile */
    private String tableName;

    /** 提取的文本内容 */
    private String text;
}