package com.ccb.techfin.service.sxd;

import com.ccb.techfin.model.sxd.dto.request.ConfirmControllerRequest;
import com.ccb.techfin.model.sxd.dto.request.SubmitMaterialsRequest;
import com.ccb.techfin.model.sxd.dto.response.ExtractStatusResponse;
import org.springframework.web.multipart.MultipartFile;

public interface SxdService {

    String uploadFile(MultipartFile file);

    String submitMaterials(SubmitMaterialsRequest request);

    void confirmControllerName(ConfirmControllerRequest request);

    boolean deleteAttachment(String attId);

    ExtractStatusResponse queryExtractStatus(String taskId);
}