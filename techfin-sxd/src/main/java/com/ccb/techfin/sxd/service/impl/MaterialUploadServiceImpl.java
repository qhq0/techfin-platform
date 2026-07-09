package com.ccb.techfin.sxd.service.impl;

import com.ccb.techfin.common.exception.BusinessException;
import com.ccb.techfin.sxd.config.ApiProperties;
import com.ccb.techfin.sxd.dto.external.AttachmentUploadResponse;
import com.ccb.techfin.sxd.dto.external.DocBatchAddItem;
import com.ccb.techfin.sxd.dto.external.DocBatchAddResponse;
import com.ccb.techfin.sxd.dto.request.UploadMaterialsRequest;
import com.ccb.techfin.sxd.dto.response.UploadMaterialsResponse;
import com.ccb.techfin.sxd.entity.ApplicationRecord;
import com.ccb.techfin.sxd.enums.TaskStatus;
import com.ccb.techfin.sxd.repository.ApplicationRecordRepository;
import com.ccb.techfin.sxd.service.MaterialUploadService;
import com.ccb.techfin.sxd.validator.FileValidator;
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
import java.util.Map;
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

        validateRequiredParams(request);
        checkCreditCodeUnique(request.getCreditCode());

        fileValidator.validate(request.getFinanceFiles(), "finance");
        fileValidator.validate(request.getBusinessFiles(), "business");

        String token = apiProperties.getDefaultToken();
        List<UploadedFileInfo> uploaded = new ArrayList<>();

        try {
            uploadFiles(request.getFinanceFiles(), "finance", token, uploaded);
            uploadFiles(request.getBusinessFiles(), "business", token, uploaded);

            if (uploaded.isEmpty()) {
                throw new BusinessException("NO_FILES", "请至少上传一个文件");
            }

            List<DocBatchAddItem> batchItems = buildBatchAddItems(request, uploaded);
            DocBatchAddResponse batchResponse = batchAddDocs(batchItems, token);

            if (!batchResponse.isSuccess() || batchResponse.getData() == null) {
                throw new BusinessException("BATCH_ADD_FAILED", "资料批量新增失败");
            }

            if (batchResponse.getData().getInvalidDocNames() != null
                    && !batchResponse.getData().getInvalidDocNames().isEmpty()) {
                log.warn("Some documents failed to add: {}", batchResponse.getData().getInvalidDocNames());
            }

            List<String> docIds = batchResponse.getData().getDocList().stream()
                    .map(DocBatchAddResponse.DocInfo::getId)
                    .collect(Collectors.toList());

            List<String> attIds = uploaded.stream()
                    .map(UploadedFileInfo::getAttId)
                    .collect(Collectors.toList());

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

            return UploadMaterialsResponse.builder()
                    .taskId(record.getTaskId())
                    .message("材料上传成功，状态：" + record.getStatus().getDescription())
                    .build();

        } catch (Exception e) {
            log.error("Upload materials failed after uploading {} files, manual cleanup may be needed",
                    uploaded.size(), e);
            throw e;
        }
    }

    private void uploadFiles(List<MultipartFile> files, String businessType, String token,
                             List<UploadedFileInfo> uploaded) {
        if (files == null) {
            return;
        }
        for (MultipartFile file : files) {
            String attId = uploadAttachment(file, token);
            String fileName = file.getOriginalFilename();
            uploaded.add(new UploadedFileInfo(attId, fileName, file.getSize(), businessType));
            log.debug("{} file uploaded: name={}, attId={}", businessType, fileName, attId);
        }
    }

    private void validateRequiredParams(UploadMaterialsRequest request) {
        if (!StringUtils.hasText(request.getCreditCode())) {
            throw new BusinessException("PARAM_MISSING", "统一社会信用代码不能为空");
        }
        if (!StringUtils.hasText(request.getCustomerNo())) {
            throw new BusinessException("PARAM_MISSING", "客户编号不能为空");
        }
        if (!request.getCreditCode().matches("^[0-9A-Z]{18}$")) {
            throw new BusinessException("INVALID_CREDIT_CODE", "统一社会信用代码格式不正确，必须为18位数字或大写字母");
        }
    }

    private String uploadAttachment(MultipartFile file, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            if (StringUtils.hasText(token)) {
                headers.set("c1-token", token);
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
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

    private List<DocBatchAddItem> buildBatchAddItems(UploadMaterialsRequest request,
                                                      List<UploadedFileInfo> uploaded) {
        List<DocBatchAddItem> items = new ArrayList<>();
        Map<String, Long> docTypeMap = apiProperties.getDocType();
        Long dirId = apiProperties.getDirId();
        Long projectId = apiProperties.getProjectId();

        for (UploadedFileInfo info : uploaded) {
            Long docTypeId = docTypeMap.get(info.getBusinessType());
            if (docTypeId == null) {
                throw new BusinessException("INVALID_BUSINESS_TYPE",
                        "未知的业务类型：" + info.getBusinessType());
            }

            String reportDate = "finance".equals(info.getBusinessType())
                    ? request.getReportDate() : null;

            DocBatchAddItem item = DocBatchAddItem.builder()
                    .attId(info.getAttId())
                    .dirId(dirId)
                    .docName(info.getFileName())
                    .docSize(info.getFileSize())
                    .docTypeId(docTypeId)
                    .extraInfo("{}")
                    .projectId(projectId)
                    .reportDate(StringUtils.hasText(reportDate) ? reportDate : null)
                    .build();

            items.add(item);
        }

        return items;
    }

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

    private void checkCreditCodeUnique(String creditCode) {
        if (repository.existsByCreditCode(creditCode)) {
            throw new BusinessException("DUPLICATE_CREDIT_CODE",
                    "统一社会信用代码 [" + creditCode + "] 已存在，请勿重复建档");
        }
    }

    private String generateTaskId() {
        UUID uuid = UUID.randomUUID();
        return "TASK-" + Long.toHexString(uuid.getMostSignificantBits())
                + Long.toHexString(uuid.getLeastSignificantBits());
    }

    @lombok.Value
    private static class UploadedFileInfo {
        String attId;
        String fileName;
        long fileSize;
        String businessType;
    }
}
