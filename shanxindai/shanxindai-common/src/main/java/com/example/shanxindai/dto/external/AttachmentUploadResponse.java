package com.example.shanxindai.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 附件上传接口响应体
 * POST /api/mdm/open/att/upload
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttachmentUploadResponse {

    /** 是否成功 */
    private boolean success;

    /** 业务码（1 表示成功） */
    private String code;

    /** 消息 */
    private String message;

    /** 附件 ID（attId），用于后续资料新增 */
    private String data;
}
