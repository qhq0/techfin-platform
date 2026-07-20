package com.ccb.techfin.service.sxd;

import com.ccb.techfin.model.sxd.dto.request.ConfirmControllerRequest;
import com.ccb.techfin.model.sxd.dto.request.SubmitMaterialsRequest;
import com.ccb.techfin.model.sxd.dto.response.ExtractDataResponse;
import com.ccb.techfin.model.sxd.dto.response.ExtractStatusResponse;
import org.springframework.web.multipart.MultipartFile;

public interface SxdService {

    String uploadFile(MultipartFile file);

    String submitMaterials(SubmitMaterialsRequest request);

    void confirmControllerName(ConfirmControllerRequest request);

    boolean deleteAttachment(String attId);

    ExtractStatusResponse queryExtractStatus(String taskId);

    ExtractDataResponse queryExtractData(String taskId);

    byte[] exportBusinessExtractData(String taskId);

    byte[] exportFinanceExtractData(String taskId);

    /**
     * 生成 Word 报告，包含企业基本信息、资产负债表关键科目和利润表关键科目。
     *
     * @param taskId 申请记录的任务 ID
     * @param cstId  客户编号（前端传入，无需查 sxd_record）
     * @return Word 文档（.docx）字节数组
     */
    byte[] generateReport(String taskId, String cstId);
}
