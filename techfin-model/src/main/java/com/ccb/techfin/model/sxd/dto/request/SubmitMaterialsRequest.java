package com.ccb.techfin.model.sxd.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 提交资料请求体。
 *
 * @author qiuhaoquan
 * @since 2026-07-23
 */
@Data
public class SubmitMaterialsRequest {

    /** 统一社会信用代码（18位数字或大写字母） */
    private String creditCode;

    /** 客户编号 */
    private String cstId;

    /** 财务报表文件列表（需携带 reportDate） */
    private List<SubmitFileItem> financeFiles;

    /** 商业计划书附件 ID（仅允许一份文件） */
    private String businessFile;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitFileItem {

        /** 附件上传返回的 attId */
        private String attId;

        /** 报告日期（仅财务报表需要） */
        private String reportDate;
    }
}
