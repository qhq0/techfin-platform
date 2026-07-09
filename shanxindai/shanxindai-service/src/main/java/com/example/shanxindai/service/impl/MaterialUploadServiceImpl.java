package com.example.shanxindai.service.impl;

import com.example.shanxindai.config.ApiProperties;
import com.example.shanxindai.dto.external.AttachmentUploadResponse;
import com.example.shanxindai.dto.external.DocBatchAddItem;
import com.example.shanxindai.dto.external.DocBatchAddResponse;
import com.example.shanxindai.dto.request.UploadMaterialsRequest;
import com.example.shanxindai.dto.response.UploadMaterialsResponse;
import com.example.shanxindai.entity.ApplicationRecord;
import com.example.shanxindai.enums.TaskStatus;
import com.example.shanxindai.exception.BusinessException;
import com.example.shanxindai.repository.ApplicationRecordRepository;
import com.example.shanxindai.service.MaterialUploadService;
import com.example.shanxindai.validator.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialUploadServiceImpl implements MaterialUploadService {

    private final ApplicationRecordRepository repository;
    private final FileValidator fileValidator;
    private final ApiProperties apiProperties;
    private final RestTemplate restTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadMaterialsResponse uploadMaterials(UploadMaterialsRequest request) {

        // ====== 1. 参数非空校验 ======
        validateRequiredParams(request);

        // ====== 1b. 唯一性校验：统一社会信用代码不可重复提交 ======
        checkCreditCodeUnique(request.getCreditCode());

        // ====== 2. 文件流校验（大小 + 格式白名单） ======
        fileValidator.validate(request.getFinanceFiles(), "finance");
        fileValidator.validate(request.getBusinessFiles(), "business");

        // 下游 API 鉴权 token，统一从配置读取
        String token = apiProperties.getDefaultToken();

        // ====== 3. 调用附件上传接口上传文件 ======
        List<UploadedFileInfo> uploaded = new ArrayList<>();

        try {
            // 上传财务报表
            if (request.getFinanceFiles() != null) {
                for (MultipartFile file : request.getFinanceFiles()) {
                    String attId = uploadAttachment(file, token);
                    uploaded.add(new UploadedFileInfo(attId, file, "finance"));
                    log.debug("Finance file uploaded: name={}, attId={}", file.getOriginalFilename(), attId);
                }
            }

            // 上传商业计划书
            if (request.getBusinessFiles() != null) {
                for (MultipartFile file : request.getBusinessFiles()) {
                    String attId = uploadAttachment(file, token);
                    uploaded.add(new UploadedFileInfo(attId, file, "business"));
                    log.debug("Business file uploaded: name={}, attId={}", file.getOriginalFilename(), attId);
                }
            }

            if (uploaded.isEmpty()) {
                throw new BusinessException("NO_FILES", "请至少上传一个文件");
            }

            // ====== 4. 调用资料批量新增接口 ======
            List<DocBatchAddItem> batchItems = buildBatchAddItems(request, uploaded);
            DocBatchAddResponse batchResponse = batchAddDocs(batchItems, token);

            if (!batchResponse.isSuccess() || batchResponse.getData() == null) {
                throw new BusinessException("BATCH_ADD_FAILED", "资料批量新增失败");
            }

            // 检查是否有无效的文档
            if (batchResponse.getData().getInvalidDocNames() != null
                    && !batchResponse.getData().getInvalidDocNames().isEmpty()) {
                log.warn("Some documents failed to add: {}", batchResponse.getData().getInvalidDocNames());
            }

            // 提取新增成功的文档 ID
            List<String> docIds = batchResponse.getData().getDocList().stream()
                    .map(DocBatchAddResponse.DocInfo::getId)
                    .collect(Collectors.toList());

            List<String> attIds = uploaded.stream()
                    .map(UploadedFileInfo::getAttId)
                    .collect(Collectors.toList());

            // ====== 5. 持久化申请记录 ======
            ApplicationRecord record = new ApplicationRecord();
            record.setTaskId(generateTaskId());
            record.setCreditCode(request.getCreditCode());
            record.setCustomerNo(request.getCustomerNo());
            record.setReportDate(request.getReportDate());
            record.setStatus(TaskStatus.PENDING_ANALYSIS);
            record.setDocIds(docIds);
            record.setAttIds(attIds);
            repository.save(record);

            log.info("Application record created: taskId={}, creditCode={}, docIds={}",
                    record.getTaskId(), record.getCreditCode(), docIds);

            // ====== 6. 返回成功响应 ======
            return UploadMaterialsResponse.builder()
                    .taskId(record.getTaskId())
                    .message("材料上传成功，状态：" + record.getStatus().getDescription())
                    .build();

        } catch (Exception e) {
            // 若中途出错，已上传的文件无法通过 API 回滚（依赖下游清理）
            // 记录日志以便人工介入
            log.error("Upload materials failed after uploading {} files, manual cleanup may be needed",
                    uploaded.size(), e);
            throw e;
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 校验必填参数
     */
    private void validateRequiredParams(UploadMaterialsRequest request) {
        if (!StringUtils.hasText(request.getCreditCode())) {
            throw new BusinessException("PARAM_MISSING", "统一社会信用代码不能为空");
        }
        if (!StringUtils.hasText(request.getCustomerNo())) {
            throw new BusinessException("PARAM_MISSING", "客户编号不能为空");
        }
        // 信用代码格式：18位大写字母或数字
        if (!request.getCreditCode().matches("^[0-9A-Z]{18}$")) {
            throw new BusinessException("INVALID_CREDIT_CODE", "统一社会信用代码格式不正确，必须为18位数字或大写字母");
        }
    }

    /**
     * 调用附件上传接口
     * POST /api/mdm/open/att/upload
     */
    private String uploadAttachment(MultipartFile file, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            if (StringUtils.hasText(token)) {
                headers.set("c1-token", token);
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            // 使用 ByteArrayResource 包装文件流，同时保留文件名
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<AttachmentUploadResponse> response = restTemplate.exchange(
                    apiProperties.getAttachmentUploadUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    AttachmentUploadResponse.class);

            AttachmentUploadResponse respBody = response.getBody();
            if (respBody == null || !respBody.isSuccess()) {
                String msg = respBody != null ? respBody.getMessage() : "未知错误";
                throw new BusinessException("ATTACH_UPLOAD_FAILED",
                        "文件 [" + file.getOriginalFilename() + "] 上传失败：" + msg);
            }

            return respBody.getData();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Attachment upload failed for file: {}", file.getOriginalFilename(), e);
            throw new BusinessException("ATTACH_UPLOAD_FAILED",
                    "文件 [" + file.getOriginalFilename() + "] 上传异常：" + e.getMessage());
        }
    }

    /**
     * 构建资料批量新增的请求体
     */
    private List<DocBatchAddItem> buildBatchAddItems(UploadMaterialsRequest request,
                                                      List<UploadedFileInfo> uploaded) {
        List<DocBatchAddItem> items = new ArrayList<>();

        for (UploadedFileInfo info : uploaded) {
            Long docTypeId = apiProperties.getDocType().get(info.getBusinessType());
            if (docTypeId == null) {
                throw new BusinessException("INVALID_BUSINESS_TYPE",
                        "未知的业务类型：" + info.getBusinessType());
            }

            DocBatchAddItem item = DocBatchAddItem.builder()
                    .attId(info.getAttId())
                    .dirId(apiProperties.getDirId())
                    .docName(info.getFile().getOriginalFilename())
                    .docSize(info.getFile().getSize())
                    .docTypeId(docTypeId)
                    .extraInfo("{}")
                    .projectId(apiProperties.getProjectId())
                    .build();

            // 财务报表需要传报告日期
            if ("finance".equals(info.getBusinessType()) && StringUtils.hasText(request.getReportDate())) {
                item.setReportDate(request.getReportDate());
            }

            items.add(item);
        }

        return items;
    }

    /**
     * 调用资料批量新增接口
     * POST /api/extract/open/doc/batch/add
     */
    private DocBatchAddResponse batchAddDocs(List<DocBatchAddItem> items, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(token)) {
                headers.set("c1-token", token);
            }

            HttpEntity<List<DocBatchAddItem>> requestEntity = new HttpEntity<>(items, headers);

            ResponseEntity<DocBatchAddResponse> response = restTemplate.exchange(
                    apiProperties.getDocBatchAddUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    DocBatchAddResponse.class);

            DocBatchAddResponse respBody = response.getBody();
            if (respBody == null) {
                throw new BusinessException("BATCH_ADD_FAILED", "资料批量新增接口返回为空");
            }

            return respBody;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Batch add documents failed", e);
            throw new BusinessException("BATCH_ADD_FAILED", "资料批量新增异常：" + e.getMessage());
        }
    }

    /**
     * 校验统一社会信用代码是否已存在（防重复建档）
     */
    private void checkCreditCodeUnique(String creditCode) {
        if (repository.existsByCreditCode(creditCode)) {
            throw new BusinessException("DUPLICATE_CREDIT_CODE",
                    "统一社会信用代码 [" + creditCode + "] 已存在，请勿重复建档");
        }
    }

    private String generateTaskId() {
        return "TASK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * 上传文件信息内部类
     */
    @lombok.Value
    private static class UploadedFileInfo {
        String attId;
        MultipartFile file;
        String businessType;
    }
}
