package com.ccb.techfin.controller.sxd;

import com.ccb.techfin.common.result.CommonResp;
import com.ccb.techfin.model.sxd.dto.request.ConfirmControllerRequest;
import com.ccb.techfin.model.sxd.dto.request.SubmitMaterialsRequest;
import com.ccb.techfin.model.sxd.dto.response.ExtractDataResponse;
import com.ccb.techfin.model.sxd.dto.response.ExtractStatusResponse;
import com.ccb.techfin.service.sxd.CustomerService;
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
    private final CustomerService customerService;

    /**
     * 上传单个附件文件到外部存储，同时将文件元信息记录到 application_att 表。
     */
    @PostMapping(value = "/upload-attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResp<String> uploadAttachment(
            @RequestParam("file") MultipartFile file) {

        String attId = sxdService.uploadFile(file);
        return CommonResp.success("附件上传成功", attId);
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
     * 提交资料：校验信用代码和客户编号，从 application_att 读取已上传附件元信息，
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
    @GetMapping("/controller-name/{customer_no}")
    public CommonResp<String> getControllerName(@PathVariable("customer_no") String customerNo) {
        String name = customerService.getControllerName(customerNo);
        return CommonResp.success(name);
    }

    /**
     * 确认/修改实际控制人姓名，回填到 application_record 表。
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
     * 内部遍历预设的 tableName，调用外部提取数据查询 API 聚合结果。
     */
    @GetMapping("/extract-data/{task_id}")
    public CommonResp<ExtractDataResponse> queryExtractData(@PathVariable("task_id") String taskId) {
        ExtractDataResponse result = sxdService.queryExtractData(taskId);
        return CommonResp.success(result);
    }
}
