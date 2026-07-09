package com.ccb.techfin.sxd.controller;

import com.ccb.techfin.common.dto.response.CommonResp;
import com.ccb.techfin.sxd.dto.request.UploadMaterialsRequest;
import com.ccb.techfin.sxd.dto.response.UploadMaterialsResponse;
import com.ccb.techfin.sxd.service.MaterialUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/sxd")
@RequiredArgsConstructor
public class MaterialUploadController {

    private final MaterialUploadService materialUploadService;

    @PostMapping(value = "/upload-materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResp<UploadMaterialsResponse> uploadMaterials(
            @RequestParam("credit_code") String creditCode,
            @RequestParam("customer_no") String customerNo,
            @RequestParam(value = "report_date", required = false) String reportDate,
            @RequestParam(value = "finance_files", required = false) List<MultipartFile> financeFiles,
            @RequestParam(value = "business_files", required = false) List<MultipartFile> businessFiles) {

        UploadMaterialsRequest request = new UploadMaterialsRequest();
        request.setCreditCode(creditCode);
        request.setCustomerNo(customerNo);
        request.setReportDate(reportDate);
        request.setFinanceFiles(financeFiles);
        request.setBusinessFiles(businessFiles);

        UploadMaterialsResponse response = materialUploadService.uploadMaterials(request);
        return CommonResp.success(response);
    }
}
