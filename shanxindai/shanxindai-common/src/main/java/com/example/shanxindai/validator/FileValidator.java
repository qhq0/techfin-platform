package com.example.shanxindai.validator;

import com.example.shanxindai.config.FileUploadConfig;
import com.example.shanxindai.exception.FileValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FileValidator {

    private final FileUploadConfig uploadConfig;

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "image/jpeg",
            "image/png"
    );

    /**
     * 批量校验文件
     *
     * @param files        待校验文件列表
     * @param businessType 业务类型（finance / business）
     */
    public void validate(List<MultipartFile> files, String businessType) {
        if (files == null || files.isEmpty()) {
            return;
        }

        List<String> allowedExts = uploadConfig.getAllowedExtensions().get(businessType);

        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();

            // 1. 检查大小
            if (file.getSize() > uploadConfig.getMaxFileSize()) {
                throw new FileValidationException(originalName, "FILE_TOO_LARGE",
                        "超过大小限制（最大 " + uploadConfig.getMaxFileSize() / (1024 * 1024) + "MB），当前大小 "
                                + file.getSize() / (1024 * 1024) + "MB");
            }

            // 2. 检查文件扩展名
            String extension = getExtension(originalName);
            if (extension == null || allowedExts.stream().noneMatch(ext -> ext.equalsIgnoreCase(extension))) {
                throw new FileValidationException(originalName, "INVALID_FILE_FORMAT",
                        "文件格式不支持，仅支持 " + String.join(", ", allowedExts));
            }

            // 3. 检查 MIME 类型（兜底）
            String contentType = file.getContentType();
            if (contentType != null && !ALLOWED_MIME_TYPES.contains(contentType)) {
                // csv 可能被识别为 text/plain，放宽校验
                if (!"csv".equalsIgnoreCase(extension)) {
                    throw new FileValidationException(originalName, "INVALID_FILE_FORMAT",
                            "文件类型不合法");
                }
            }
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
