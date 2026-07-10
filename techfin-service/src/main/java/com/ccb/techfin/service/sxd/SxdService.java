package com.ccb.techfin.service.sxd;

import com.ccb.techfin.model.sxd.dto.request.UploadMaterialsRequest;
import com.ccb.techfin.model.sxd.dto.response.UploadMaterialsResponse;

public interface SxdService {

    UploadMaterialsResponse uploadMaterials(UploadMaterialsRequest request);
}
