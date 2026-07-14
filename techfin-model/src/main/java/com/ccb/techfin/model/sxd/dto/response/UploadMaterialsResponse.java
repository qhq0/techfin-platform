package com.ccb.techfin.model.sxd.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadMaterialsResponse {

    /** 任务 ID */
    private String taskId;

    /** 提示消息 */
    private String message;

    /** 批量提交时，成功提交的数量 */
    private Integer submittedCount;

    /** 批量提交时，提交失败的 ID 列表及原因 */
    private List<FailedItem> failedIds;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedItem {
        /** 提交失败的记录 ID */
        private String id;
        /** 失败原因 */
        private String reason;
    }
}
