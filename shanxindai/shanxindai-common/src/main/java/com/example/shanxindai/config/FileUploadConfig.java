package com.example.shanxindai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadConfig {

    /** 单文件大小上限（字节），默认 50MB */
    private long maxFileSize = 50 * 1024 * 1024L;

    /** 允许的文件后缀白名单（按业务类型） */
    private Map<String, List<String>> allowedExtensions;

    /** 接口超时秒数 */
    private int timeoutSeconds = 120;
}
