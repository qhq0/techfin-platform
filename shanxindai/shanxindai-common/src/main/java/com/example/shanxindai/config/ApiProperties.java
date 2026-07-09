package com.example.shanxindai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 外部 API 配置：附件上传、资料批量新增等接口的地址与业务常量
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "api")
public class ApiProperties {

    /** API 网关基础地址 */
    private String baseUrl;

    /** 附件上传接口地址 */
    private String attachmentUploadUrl;

    /** 资料批量新增接口地址 */
    private String docBatchAddUrl;

    /** 默认项目 ID */
    private Long projectId;

    /** 默认目录 ID */
    private Long dirId = 0L;

    /** 文档类型 ID 映射：finance -> docTypeId, business -> docTypeId */
    private Map<String, Long> docType;

    /** c1-token，配置文件统一设置 */
    private String defaultToken = "";
}
