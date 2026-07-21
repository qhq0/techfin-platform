package com.ccb.techfin.controller.sxd;

import com.ccb.techfin.common.result.CommonResp;
import com.ccb.techfin.model.sxd.dto.request.ConfirmControllerRequest;
import com.ccb.techfin.model.sxd.dto.request.ReportRequest;
import com.ccb.techfin.model.sxd.dto.request.SubmitMaterialsRequest;
import com.ccb.techfin.model.sxd.dto.response.ExtractDataResponse;
import com.ccb.techfin.model.sxd.dto.response.ExtractStatusResponse;
import com.ccb.techfin.service.sxd.CustomerService;
import com.ccb.techfin.service.sxd.ExtractDataService;
import com.ccb.techfin.service.sxd.SxdService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/sxd")
@RequiredArgsConstructor
public class SxdController {

    private final SxdService sxdService;
    private final ExtractDataService extractDataService;
    private final CustomerService customerService;

    /**
     * 上传单个附件文件到外部存储，同时将文件元信息记录到 sxd_att 表。
     */
    @PostMapping(value = "/upload-attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResp<String> uploadAttachment(
            @RequestParam("file") MultipartFile file) {

        String attId = sxdService.uploadFile(file);
        return CommonResp.success("附件上传成功", attId);
    }

    /**
     * 删除单个附件记录（从 sxd_att 表中删除）。
     */
    @DeleteMapping("/delete-attachment/{att_id}")
    public CommonResp<Void> deleteAttachment(@PathVariable("att_id") String attId) {
        boolean deleted = sxdService.deleteAttachment(attId);
        if (deleted) {
            return CommonResp.success("附件删除成功", null);
        } else {
            return CommonResp.fail(1, "附件不存在或已删除");
        }
    }

    /**
     * 提交资料：校验信用代码和客户编号，从 sxd_att 读取已上传附件元信息，
     * 创建以 taskId 为主键的申请记录，批量新增文档。
     */
    @PostMapping("/submit-materials")
    public CommonResp<String> submitMaterials(@RequestBody SubmitMaterialsRequest request) {
        String taskId = sxdService.submitMaterials(request);
        return CommonResp.success("资料提交成功", taskId);
    }

    /**
     * 根据客户编号查询实控人姓名。
     */
    @GetMapping("/controller-name/{cst_id}")
    public CommonResp<String> getControllerName(@PathVariable("cst_id") String cstId) {
        String name = customerService.getControllerName(cstId);
        return CommonResp.success(name);
    }

    /**
     * 校验当前用户是否拥有该客户的管户权。
     */
    @PostMapping("/cust-ownership")
    public CommonResp<Boolean> getCustOwnership(@RequestBody ReportRequest request,
                                                 HttpServletRequest servletRequest) {
        String userId = (String) servletRequest.getAttribute("userId");
        boolean hasOwnership = customerService.getCustOwnership(
                request.getTaskId(), request.getCstId(), userId);
        return CommonResp.success(hasOwnership);
    }


    /**
     * 确认/修改实际控制人姓名，回填到 sxd_record 表。
     */
    @PutMapping("/application-record/controller-name")
    public CommonResp<Void> confirmControllerName(@RequestBody ConfirmControllerRequest request) {
        sxdService.confirmControllerName(request);
        return CommonResp.success("实际控制人确认成功", null);
    }

    /**
     * 查询资料提取状态。
     * 遍历该任务下所有文档，调用外部资料详情接口获取提取状态。
     * 全部完成时返回 completed=true，否则返回 false 和待处理的文档名称。
     */
    @GetMapping("/extract-status/{task_id}")
    public CommonResp<ExtractStatusResponse> queryExtractStatus(@PathVariable("task_id") String taskId) {
        ExtractStatusResponse result = sxdService.queryExtractStatus(taskId);
        return CommonResp.success(result);
    }

    /**
     * 查询商业计划书的提取数据。
     * 前端应在提取状态 completed=true 后调用此接口，获取各 section 的提取文本。
     * 先从缓存表读取，缓存未命中时调用外部 API 并写入缓存。
     */
    @GetMapping("/extract-data/business/{task_id}")
    public CommonResp<ExtractDataResponse> queryExtractData(@PathVariable("task_id") String taskId) {
        ExtractDataResponse result = extractDataService.queryExtractData(taskId);
        return CommonResp.success(result);
    }

    /**
     * 导出商业计划书的提取结果 xlsx 文件。
     * 根据 taskId 查询 sxd_doc 中商业计划书类型的文档，调用外部导出资料接口直接返回文件流。
     */
    @GetMapping("/export-data/business/{task_id}")
    public ResponseEntity<byte[]> exportBusinessData(@PathVariable("task_id") String taskId) {
        byte[] data = extractDataService.exportBusinessExtractData(taskId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"商业计划书提取数据.xlsx\"")
                .body(data);
    }

    /**
     * 导出财务报表的提取结果 zip 压缩包。
     * 根据 taskId 查询 sxd_doc 中财务报表类型的文档列表，逐个调用外部导出资料接口，
     * 将所有 xlsx 打包为 zip 返回。
     */
    @GetMapping("/export-data/finance/{task_id}")
    public ResponseEntity<byte[]> exportFinanceData(@PathVariable("task_id") String taskId) {
        byte[] data = extractDataService.exportFinanceExtractData(taskId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"财务报表提取数据.zip\"")
                .body(data);
    }

    /**
     * 生成 Word 报告，包含企业基本信息、商业计划书提取文本、资产负债表关键科目和利润表关键科目。
     */
    @PostMapping("/report")
    public ResponseEntity<byte[]> generateReport(@RequestBody ReportRequest request) {
        byte[] data = extractDataService.generateReport(request.getTaskId(), request.getCstId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"企业信息报告.docx\"")
                .body(data);
    }
}