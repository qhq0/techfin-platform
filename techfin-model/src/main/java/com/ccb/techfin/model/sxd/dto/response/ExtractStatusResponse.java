package com.ccb.techfin.model.sxd.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 资料提取状态查询响应。
 * 前端通过 completed 判断是否全部完成，pendingDocNames 列出仍在处理中的文档名称。
 */
@Data
@AllArgsConstructor
public class ExtractStatusResponse {

    /** true=全部完成（extractState 均为 F 或 Y），false=仍有文档待处理 */
    private boolean completed;

    /** 状态为 U（待执行）或 I（执行中）的文档名称列表 */
    private List<String> pendingDocNames;
}
