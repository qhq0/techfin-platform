package com.ccb.techfin.service.sxd;

import com.ccb.techfin.model.sxd.dto.request.SubmitMaterialsRequest;
import com.ccb.techfin.model.sxd.dto.response.FileUploadResult;
import com.ccb.techfin.model.sxd.dto.response.UploadMaterialsResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SxdService {

    FileUploadResult uploadFinanceFile(MultipartFile financeFile);

    FileUploadResult uploadBusinessFile(MultipartFile businessFile);

    UploadMaterialsResponse submitMaterials(SubmitMaterialsRequest request);

    /**
     * 删除附件记录。
     *
     * @param attId 待删除的附件 ID
     * @return 是否删除成功
     */
    boolean deleteAttachment(String attId);
}
