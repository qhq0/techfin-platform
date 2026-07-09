package com.ccb.techfin.sxd.service;

import com.ccb.techfin.sxd.dto.request.UploadMaterialsRequest;
import com.ccb.techfin.sxd.dto.response.UploadMaterialsResponse;

public interface MaterialUploadService {

    UploadMaterialsResponse uploadMaterials(UploadMaterialsRequest request);
}
