package com.ccb.techfin.controller.sxd;

import com.ccb.techfin.common.result.CommonResp;
import com.ccb.techfin.model.sxd.dto.request.SubmitMaterialsRequest;
import com.ccb.techfin.model.sxd.dto.response.FileUploadResult;
import com.ccb.techfin.model.sxd.dto.response.UploadMaterialsResponse;
import com.ccb.techfin.service.sxd.SxdService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/sxd")
@RequiredArgsConstructor
public class SxdController {

    private final SxdService sxdService;

    /**
     * 上传单个财务报表文件到外部存储，同时将文件元信息记录到 application_att 表。
     */
    @PostMapping(value = "/upload-finance-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResp<String> uploadFinanceFile(
            @RequestParam("finance_file") MultipartFile financeFile) {

        FileUploadResult result = sxdService.uploadFinanceFile(financeFile);
        return CommonResp.success("财务报表上传成功", result.getAttId());
    }

    /**
     * 上传单个商业计划书文件到外部存储，同时将文件元信息记录到 application_att 表。
     */
    @PostMapping(value = "/upload-business-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResp<String> uploadBusinessFile(
            @RequestParam("business_file") MultipartFile businessFile) {

        FileUploadResult result = sxdService.uploadBusinessFile(businessFile);
        return CommonResp.success("商业计划书上传承成功", result.getAttId());
    }

    /**
     * 删除单个附件记录（从 application_att 表中删除）。
     */
    @DeleteMapping("/delete-attachment/{att_id}")
    public CommonResp<Void> deleteAttachment(@PathVariable("att_id") String attId) {
        boolean deleted = sxdService.deleteAttachment(attId);
        if (deleted) {
            return CommonResp.success("附件删除成功", null);
        } else {
            return CommonResp.error(1, "附件不存在或已删除");
        }
    }

    /**
     * 提交资料：校验信用代码和客户编号，从 application_att 读取已上传附件，
     * 创建以 taskId 为主键的申请记录，批量新增文档。
     */
    @PostMapping("/submit-materials")
    public CommonResp<UploadMaterialsResponse> submitMaterials(@RequestBody SubmitMaterialsRequest request) {
        UploadMaterialsResponse response = sxdService.submitMaterials(request);
        return CommonResp.success(response);
    }
}
