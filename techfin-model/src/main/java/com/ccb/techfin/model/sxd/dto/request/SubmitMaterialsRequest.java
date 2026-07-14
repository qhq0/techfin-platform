package com.ccb.techfin.model.sxd.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class SubmitMaterialsRequest {

    /** 统一社会信用代码（18位数字或大写字母） */
    private String creditCode;

    /** 客户编号 */
    private String customerNo;

    /** 财务报表文件列表（需携带 reportDate） */
    private List<SubmitFileItem> financeFiles;

    /** 商业计划书文件列表 */
    private List<SubmitFileItem> businessFiles;

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
