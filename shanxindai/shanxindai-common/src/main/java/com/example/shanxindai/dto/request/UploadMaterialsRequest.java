package com.example.shanxindai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class UploadMaterialsRequest {

    @NotBlank(message = "统一社会信用代码不能为空")
    @Size(min = 18, max = 18, message = "统一社会信用代码必须为18位")
    @Pattern(regexp = "^[0-9A-Z]{18}$", message = "统一社会信用代码必须为18位数字或大写字母")
    private String creditCode;

    @NotBlank(message = "客户编号不能为空")
    private String customerNo;

    /** 财报报告日期（格式 YYYY-MM-DD，上传财务报表时必填） */
    private String reportDate;

    /** 企业财务报表文件列表 */
    private java.util.List<MultipartFile> financeFiles;

    /** 商业计划书文件列表 */
    private java.util.List<MultipartFile> businessFiles;
}
