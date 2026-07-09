package com.example.shanxindai.service;

import com.example.shanxindai.dto.request.UploadMaterialsRequest;
import com.example.shanxindai.dto.response.UploadMaterialsResponse;

public interface MaterialUploadService {

    /**
     * 上传企业材料（步骤1）
     *
     * @param request 请求参数（信用代码、客户编号、文件列表等）
     * @return 任务 ID 及状态
     */
    UploadMaterialsResponse uploadMaterials(UploadMaterialsRequest request);
}
