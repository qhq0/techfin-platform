package com.example.shanxindai.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 资料批量新增接口响应体
 * POST /api/extract/open/doc/batch/add
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocBatchAddResponse {

    /** 是否成功 */
    private boolean success;

    /** 业务码（1 表示成功） */
    private String code;

    /** 消息 */
    private String message;

    /** 响应数据 */
    private DataBody data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataBody {

        /** 新增失败的文档名称列表 */
        private List<String> invalidDocNames;

        /** 新增成功的文档列表 */
        private List<DocInfo> docList;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DocInfo {

        /** 文档 ID（docId） */
        private String id;

        /** 文档名称 */
        private String docName;

        /** 项目 ID */
        private String projectId;

        /** 附件 ID */
        private String attId;
    }
}
