package com.ccb.techfin.service.sxd;

import com.ccb.techfin.model.sxd.dto.request.ConfirmControllerRequest;
import com.ccb.techfin.model.sxd.dto.request.SubmitMaterialsRequest;
import com.ccb.techfin.model.sxd.dto.response.ExtractDataResponse;
import com.ccb.techfin.model.sxd.dto.response.ExtractStatusResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SxdService {

    String uploadFile(MultipartFile file);

    String submitMaterials(SubmitMaterialsRequest request);

    /**
     * 确认/修改实际控制人姓名并回填 application_record。
     *
     * @param request 任务 ID + 实控人姓名
     */
    void confirmControllerName(ConfirmControllerRequest request);

    /**
     * 删除附件记录。
     *
     * @param attId 待删除的附件 ID
     * @return 是否删除成功
     */
    boolean deleteAttachment(String attId);

    /**
     * 查询资料提取状态。
     * 通过 taskId 从 application_doc 获取文档列表，调用外部资料详情接口逐个查询提取状态。
     * 所有文档 extractState 均为 F 或 Y 时视为完成，否则返回 false 及待处理的文档名称。
     *
     * @param taskId 申请记录的任务 ID
     * @return 提取状态结果（completed + pendingDocNames）
     */
    ExtractStatusResponse queryExtractStatus(String taskId);

    /**
     * 查询商业计划书的提取数据。
     * 根据 taskId 从 application_doc 查询商业计划书文档列表，遍历预设的 tableName，
     * 调用外部提取数据查询 API，聚合各部分的文本内容返回给前端。
     *
     * @param taskId 申请记录的任务 ID
     * @return 提取数据列表（tableName → text）
     */
    ExtractDataResponse queryExtractData(String taskId);
}
