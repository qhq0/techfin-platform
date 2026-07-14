package com.ccb.techfin.service.sxd.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccb.techfin.common.exception.BusinessException;
import com.ccb.techfin.dao.sxd.AttachmentMapper;
import com.ccb.techfin.dao.sxd.DocEntryMapper;
import com.ccb.techfin.dao.sxd.SxdMapper;
import com.ccb.techfin.model.sxd.dto.external.DocBatchAddData;
import com.ccb.techfin.model.sxd.dto.external.DocBatchAddItem;
import com.ccb.techfin.model.sxd.dto.external.DocInfo;
import com.ccb.techfin.model.sxd.dto.external.ExternalResponse;
import com.ccb.techfin.model.sxd.dto.request.SubmitMaterialsRequest;
import com.ccb.techfin.model.sxd.dto.response.FileUploadResult;
import com.ccb.techfin.model.sxd.dto.response.UploadMaterialsResponse;
import com.ccb.techfin.model.sxd.entity.ApplicationAttachment;
import com.ccb.techfin.model.sxd.entity.ApplicationRecord;
import com.ccb.techfin.model.sxd.entity.DocEntry;
import com.ccb.techfin.model.sxd.enums.TaskStatus;
import com.ccb.techfin.service.sxd.SxdService;
import com.ccb.techfin.service.sxd.config.ApiProperties;
import com.ccb.techfin.service.sxd.validator.FileValidator;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SxdServiceImpl implements SxdService {

    private final SxdMapper sxdMapper;
    private final AttachmentMapper attachmentMapper;
    private final DocEntryMapper docEntryMapper;
    private final FileValidator fileValidator;
    private final ApiProperties apiProperties;
    private final RestTemplate restTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResult uploadFinanceFile(MultipartFile file) {
        fileValidator.validate(Collections.singletonList(file), "finance");
        String token = apiProperties.getDefaultToken();
        String attId = uploadAttachment(file, token);

        ApplicationAttachment record = new ApplicationAttachment();
        record.setAttId(attId);
        record.setFileName(file.getOriginalFilename());
        record.setFileSize(file.getSize());
        record.setBusinessType("finance");
        attachmentMapper.insert(record);

        log.info("Finance file uploaded: attId={}, fileName={}", attId, file.getOriginalFilename());
        return FileUploadResult.builder()
                .attId(attId)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResult uploadBusinessFile(MultipartFile file) {
        fileValidator.validate(Collections.singletonList(file), "business");
        String token = apiProperties.getDefaultToken();
        String attId = uploadAttachment(file, token);

        ApplicationAttachment record = new ApplicationAttachment();
        record.setAttId(attId);
        record.setFileName(file.getOriginalFilename());
        record.setFileSize(file.getSize());
        record.setBusinessType("business");
        attachmentMapper.insert(record);

        log.info("Business file uploaded: attId={}, fileName={}", attId, file.getOriginalFilename());
        return FileUploadResult.builder()
                .attId(attId)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadMaterialsResponse submitMaterials(SubmitMaterialsRequest request) {
        validateRequiredParams(request.getCreditCode(), request.getCustomerNo());
        checkCreditCodeUnique(request.getCreditCode());

        // 从请求体收集文件项，标记 businessType
        List<SubmitFileMeta> allItems = new ArrayList<>();
        if (request.getFinanceFiles() != null) {
            for (SubmitMaterialsRequest.SubmitFileItem f : request.getFinanceFiles()) {
                allItems.add(new SubmitFileMeta(f.getAttId(), "finance", f.getReportDate()));
            }
        }
        if (request.getBusinessFiles() != null) {
            for (SubmitMaterialsRequest.SubmitFileItem f : request.getBusinessFiles()) {
                allItems.add(new SubmitFileMeta(f.getAttId(), "business", null));
            }
        }
        if (allItems.isEmpty()) {
            throw new BusinessException("NO_FILES", "请至少提供一个文件");
        }

        String batchTaskId = generateTaskId();
        String token = apiProperties.getDefaultToken();

        // 创建申请记录（以 taskId 为主键）
        LocalDateTime now = LocalDateTime.now();
        ApplicationRecord record = new ApplicationRecord();
        record.setTaskId(batchTaskId);
        record.setCreditCode(request.getCreditCode());
        record.setCustomerNo(request.getCustomerNo());
        record.setStatus(TaskStatus.PENDING_ANALYSIS);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        sxdMapper.insert(record);

        try {
            // 构建批量新增参数（从 application_att 查文件名和大小）
            List<DocBatchAddItem> batchItems = buildBatchAddItems(allItems);
            ExternalResponse batchResponse = batchAddDocs(batchItems, token);
            if (!batchResponse.isSuccess() || batchResponse.getData() == null) {
                throw new BusinessException("BATCH_ADD_FAILED",
                        "申请记录 " + batchTaskId + " 资料批量新增失败");
            }

            DocBatchAddData batchData = batchResponse.getDataAs(DocBatchAddData.class);
            if (batchData.getInvalidDocNames() != null
                    && !batchData.getInvalidDocNames().isEmpty()) {
                log.warn("Some documents failed to add for taskId={}: {}",
                        batchTaskId, batchData.getInvalidDocNames());
            }

            // 通过 attId 匹配请求体中的 reportDate 和 businessType，创建 DocEntry 并插入
            Map<String, SubmitFileMeta> itemIndex = allItems.stream()
                    .collect(Collectors.toMap(i -> i.attId, i -> i, (a, b) -> a));
            for (DocInfo doc : batchData.getDocList()) {
                SubmitFileMeta matched = itemIndex.get(doc.getAttId());
                DocEntry entry = new DocEntry(
                        doc.getId(),
                        batchTaskId,
                        matched != null ? matched.businessType : null,
                        matched != null ? matched.reportDate : null);
                docEntryMapper.insert(entry);
            }

            log.info("Application record submitted: taskId={}, creditCode={}, docCount={}",
                    batchTaskId, record.getCreditCode(), batchData.getDocList().size());

            return UploadMaterialsResponse.builder()
                    .taskId(batchTaskId)
                    .submittedCount(1)
                    .message("资料提交成功")
                    .failedIds(Collections.emptyList())
                    .build();

        } catch (BusinessException e) {
            log.warn("Submission failed for taskId={}: {}", batchTaskId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error submitting taskId={}", batchTaskId, e);
            throw new BusinessException("BATCH_ADD_FAILED",
                    "资料批量新增异常：" + e.getMessage());
        }
    }

    private void validateRequiredParams(String creditCode, String customerNo) {
        if (!StringUtils.hasText(creditCode)) {
            throw new BusinessException("PARAM_MISSING", "统一社会信用代码不能为空");
        }
        if (!StringUtils.hasText(customerNo)) {
            throw new BusinessException("PARAM_MISSING", "客户编号不能为空");
        }
        if (!creditCode.matches("^[0-9A-Z]{18}$")) {
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
            ResponseEntity<ExternalResponse> response = restTemplate.exchange(
                    apiProperties.getAttachmentUploadUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    ExternalResponse.class);
            ExternalResponse respBody = response.getBody();
            if (respBody == null || !respBody.isSuccess()) {
                String msg = respBody != null ? respBody.getMessage() : "未知错误";
                throw new BusinessException("ATTACH_UPLOAD_FAILED",
                        "文件 [" + file.getOriginalFilename() + "] 上传失败：" + msg);
            }
            return (String) respBody.getData();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Attachment upload failed for file: {}", file.getOriginalFilename(), e);
            throw new BusinessException("ATTACH_UPLOAD_FAILED",
                    "文件 [" + file.getOriginalFilename() + "] 上传异常：" + e.getMessage());
        }
    }

    /**
     * 根据请求中的文件项列表构建批量新增请求参数。
     * fileName/fileSize 从 application_att 表查询。
     */
    private List<DocBatchAddItem> buildBatchAddItems(List<SubmitFileMeta> items) {
        List<DocBatchAddItem> result = new ArrayList<>();
        Map<String, Long> docTypeMap = apiProperties.getDocType();
        Long dirId = apiProperties.getDirId();
        Long projectId = apiProperties.getProjectId();
        for (SubmitFileMeta item : items) {
            Long docTypeId = docTypeMap.get(item.businessType);
            if (docTypeId == null) {
                throw new BusinessException("INVALID_BUSINESS_TYPE",
                        "未知的业务类型：" + item.businessType);
            }
            // 从 application_att 查询文件元信息
            ApplicationAttachment att = attachmentMapper.selectOne(
                    new LambdaQueryWrapper<ApplicationAttachment>()
                            .eq(ApplicationAttachment::getAttId, item.attId));
            if (att == null) {
                throw new BusinessException("ATTACH_NOT_FOUND",
                        "附件 " + item.attId + " 不存在，请重新上传");
            }
            result.add(DocBatchAddItem.builder()
                    .attId(item.attId)
                    .dirId(dirId)
                    .docName(att.getFileName())
                    .docSize(att.getFileSize())
                    .docTypeId(docTypeId)
                    .extraInfo("{}")
                    .projectId(projectId)
                    .reportDate(item.reportDate)
                    .build());
        }
        return result;
    }

    private ExternalResponse batchAddDocs(List<DocBatchAddItem> items, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(token)) {
                headers.set("c1-token", token);
            }
            HttpEntity<List<DocBatchAddItem>> requestEntity = new HttpEntity<>(items, headers);
            ResponseEntity<ExternalResponse> response = restTemplate.exchange(
                    apiProperties.getDocBatchAddUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    ExternalResponse.class);
            ExternalResponse respBody = response.getBody();
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
        Long count = sxdMapper.selectCount(
                new LambdaQueryWrapper<ApplicationRecord>()
                        .eq(ApplicationRecord::getCreditCode, creditCode));
        if (count > 0) {
            throw new BusinessException("DUPLICATE_CREDIT_CODE",
                    "统一社会信用代码 [" + creditCode + "] 已存在，请勿重复建档");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAttachment(String attId) {
        if (!StringUtils.hasText(attId)) {
            return false;
        }
        int deleted = attachmentMapper.delete(
                new LambdaQueryWrapper<ApplicationAttachment>()
                        .eq(ApplicationAttachment::getAttId, attId));
        boolean success = deleted > 0;
        if (success) {
            log.info("Attachment deleted: attId={}", attId);
        } else {
            log.warn("Attachment not found for deletion: attId={}", attId);
        }
        return success;
    }

    private String generateTaskId() {
        UUID uuid = UUID.randomUUID();
        return "TASK-" + String.format("%016x%016x",
                uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    /** 提交时从请求体解析的文件项（attId + businessType + reportDate） */
    @lombok.AllArgsConstructor
    private static class SubmitFileMeta {
        final String attId;
        final String businessType;
        final String reportDate;
    }
}
