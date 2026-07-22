package com.ccb.techfin.model.sxd.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 资产负债表提取数据查询（queryData）的 data 数组中单条记录。
 * 对应外部 API 返回的资产负债表科目数据项。
 *
 * queryData 接口（POST /api/extract/open/doc/queryData）返回的 data 字段为 snake_case
 * （如 item_standard、current_amount、report_date），故显式标注 SnakeCaseStrategy 以匹配。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceSheetRecord {

    /** 标准科目名称，如 营业总收入、营业成本（用于模糊匹配） */
    private String itemStandard;

    /** 科目编码，如 B1100101BB */
    private String itemCode;

    /** 备注 */
    private String note;

    /** 科目名称，如 货币资金 */
    private String item;

    /** 本期金额 */
    private BigDecimal currentAmount;

    /** 上期金额 */
    private BigDecimal lastAmount;

    /** 报表日期，如 2025-06-30 */
    private String reportDate;

    /** 币种单位 */
    private String unit;

    /** 文档 ID */
    private String docId;
}
