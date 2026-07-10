package com.ccb.techfin.model.sxd.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class UploadMaterialsRequest {

    private String creditCode;
    private String customerNo;
    private String reportDate;
    private List<MultipartFile> financeFiles;
    private List<MultipartFile> businessFiles;
}
