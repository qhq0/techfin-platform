package com.ccb.techfin.service.sxd.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadConfig {

    private long maxFileSize = 50 * 1024 * 1024L;
    private Map<String, List<String>> allowedExtensions;
    private int timeoutSeconds = 120;
}
