package com.ccb.techfin.service.sxd;

import com.ccb.techfin.model.sxd.dto.request.ConfirmControllerRequest;
import com.ccb.techfin.model.sxd.dto.request.SubmitMaterialsRequest;
import com.ccb.techfin.model.sxd.dto.response.ExtractStatusResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 善新贷业务服务接口：材料上传、提交、附件管理、实控人确认、提取状态轮询。
 *
 * @author qiuhaoquan
 * @since 2026-07-23
 */
public interface SxdService {

    String uploadFile(MultipartFile file);

    String submitMaterials(SubmitMaterialsRequest request);

    void confirmControllerName(ConfirmControllerRequest request);

    boolean deleteAttachment(String attId);

    ExtractStatusResponse queryExtractStatus(String taskId);
}