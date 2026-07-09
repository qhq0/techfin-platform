package com.example.shanxindai.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 资料批量新增接口的请求项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocBatchAddItem {

    /** 附件上传接口返回的 attId */
    private String attId;

    /** 目录 ID，默认 0 */
    private Long dirId;

    /** 文档名称 */
    private String docName;

    /** 文件大小（字节） */
    private Long docSize;

    /** 文档类型 ID */
    private Long docTypeId;

    /** 扩展信息 */
    private String extraInfo;

    /** 项目 ID */
    private Long projectId;

    /** 报告日期（财务报表使用） */
    private String reportDate;
}
