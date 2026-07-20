package com.ccb.techfin.service.sxd.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccb.techfin.common.exception.BusinessException;
import com.ccb.techfin.dao.sxd.AttachmentMapper;
import com.ccb.techfin.dao.sxd.DocEntryMapper;
import com.ccb.techfin.dao.sxd.SxdMapper;
import com.ccb.techfin.model.sxd.dto.external.DocBatchAddData;
import com.ccb.techfin.model.sxd.dto.external.DocBatchAddItem;
import com.ccb.techfin.model.sxd.dto.external.DocDetailData;
import com.ccb.techfin.model.sxd.dto.external.DocInfo;
import com.ccb.techfin.model.sxd.dto.external.ExternalResponse;
import com.ccb.techfin.model.sxd.dto.request.ConfirmControllerRequest;
import com.ccb.techfin.model.sxd.dto.request.SubmitMaterialsRequest;
import com.ccb.techfin.model.sxd.dto.external.ExtractQueryDataRecord;
import com.ccb.techfin.model.sxd.dto.external.ExtractQueryDataRequest;
import com.ccb.techfin.model.sxd.dto.external.BalanceSheetRecord;
import com.ccb.techfin.model.sxd.dto.external.DocTableStateRecord;
import com.ccb.techfin.model.sxd.dto.external.AuditReportItem;
import com.ccb.techfin.model.sxd.dto.response.ExtractDataResponse;
import com.ccb.techfin.model.sxd.dto.response.ExtractStatusResponse;
import com.ccb.techfin.model.sxd.entity.ApplicationAttachment;
import com.ccb.techfin.model.sxd.entity.ApplicationRecord;
import com.ccb.techfin.model.sxd.entity.DocEntry;
import com.ccb.techfin.model.sxd.entity.CustomerProfile;
import com.ccb.techfin.model.sxd.enums.TaskStatus;
import com.ccb.techfin.service.sxd.SxdService;
import com.ccb.techfin.service.sxd.CustomerService;
import com.ccb.techfin.service.sxd.config.ApiProperties;
import com.ccb.techfin.service.sxd.validator.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
    private final CustomerService customerService;
    private final ResourceLoader resourceLoader;

    /** 商业计划书提取数据查询的 tableName 列表（按展示顺序） */
    private static final List<String> BUSINESS_PLAN_TABLES = Collections.unmodifiableList(
            Arrays.asList(
                    "dib_manage_company_profile",
                    "dib_director_keyresume",
                    "dib_manage_business_and_products",
                    "dib_manage_business_circumstance",
                    "dib_company_qualification",
                    "dib_manage_progressiveness_description",
                    "dib_manage_competitive_advantages",
                    "dib_manage_development_strategy",
                    "dib_manage_y_industry_analysis"
            ));

    /** 资产负债表关键科目名称（按展示顺序） */
    private static final List<String> BALANCE_SHEET_KEY_ITEMS = Collections.unmodifiableList(
            Arrays.asList(
                    "资产总计", "流动资产", "应收账款", "预付款项", "其他应收款", "存货", "固定资产",
                    "负债合计", "流动负债", "短期借款", "应付账款", "预收款项", "非流动负债", "长期借款",
                    "所有者权益", "实收资本", "未分配利润"
            ));

    /** 利润表关键科目名称（按展示顺序，不含计算行） */
    private static final List<String> PROFIT_SHEET_KEY_ITEMS = Collections.unmodifiableList(
            Arrays.asList(
                    "营业收入", "营业成本", "管理费用", "销售费用", "财务费用", "研发费用",
                    "营业利润", "利润总额", "净利润"
            ));

    /** 利润表科目匹配关键词：显示名称 → 可能的 item_standard / item 值 */
    private static final Map<String, List<String>> PROFIT_ITEM_SEARCH_KEYS = Map.of(
            "营业收入", Arrays.asList("营业总收入", "营业收入"),
            "营业成本", Arrays.asList("营业成本"),
            "管理费用", Arrays.asList("管理费用"),
            "销售费用", Arrays.asList("销售费用"),
            "财务费用", Arrays.asList("财务费用"),
            "研发费用", Arrays.asList("研发费用"),
            "营业利润", Arrays.asList("营业利润"),
            "利润总额", Arrays.asList("利润总额"),
            "净利润", Arrays.asList("净利润")
    );

    /** 占位符名称 -> CustomerProfile 字段值提取函数 */
    private static final Map<String, Function<CustomerProfile, String>> PROFILE_FIELD_GETTERS = Map.ofEntries(
            Map.entry("cst_nm", CustomerProfile::getCstNm),
            Map.entry("credit_code", CustomerProfile::getCreditCode),
            Map.entry("fd_dt", CustomerProfile::getFdDt),
            Map.entry("lgl_rprs_nm", CustomerProfile::getLglRprsNm),
            Map.entry("act_cntlr_nm", CustomerProfile::getActCntlrNm),
            Map.entry("rgst_cpamt", CustomerProfile::getRgstCpamt),
            Map.entry("arcptl_cpamt", CustomerProfile::getArcptlCpamt),
            Map.entry("cpct_tpcd", CustomerProfile::getCpctTpcd),
            Map.entry("entp_sz_cd", CustomerProfile::getEntpSzCd),
            Map.entry("entp_bliy", CustomerProfile::getEntpBliy),
            Map.entry("dtl_adr", CustomerProfile::getDtlAdr),
            Map.entry("org_oprt_scop_dsc", CustomerProfile::getOrgOprtScopDsc),
            Map.entry("tech_tag", CustomerProfile::getTechTag),
            Map.entry("tech_flow", CustomerProfile::getTechFlow),
            Map.entry("kc_score", CustomerProfile::getKcScore),
            Map.entry("entp_ptnt_num", CustomerProfile::getEntpPtntNum),
            Map.entry("entp_prct_new_tp_ptnt_num", CustomerProfile::getEntpPrctNewTpPtntNum),
            Map.entry("entp_ivt_ptnt_num", CustomerProfile::getEntpIvtPtntNum),
            Map.entry("clst_5yr_inn_rs_wcopr_num", CustomerProfile::getClst5YrInnRsWcoprNum),
            Map.entry("if_loan", CustomerProfile::getIfLoan),
            Map.entry("product_name", CustomerProfile::getProductName),
            Map.entry("loan_amount", CustomerProfile::getLoanAmount),
            Map.entry("loan_term", CustomerProfile::getLoanTerm),
            Map.entry("loan_balance", CustomerProfile::getLoanBalance),
            Map.entry("dep_bal", CustomerProfile::getDepBal),
            Map.entry("dep_bal_dt", CustomerProfile::getDepBalDt),
            Map.entry("dep_aadbal", CustomerProfile::getDepAadbal),
            Map.entry("acc_start_dt", CustomerProfile::getAccStartDt),
            Map.entry("acc_type", CustomerProfile::getAccType),
            Map.entry("isug_pnum", CustomerProfile::getIsugPnum),
            Map.entry("avg_12_isug_amt", CustomerProfile::getAvg12IsugAmt),
            Map.entry("if_yuqi", CustomerProfile::getIfYuqi),
            Map.entry("ltgtrltd_ind", CustomerProfile::getLtgtrltdInd),
            Map.entry("if_rad_alarm", CustomerProfile::getIfRadAlarm)
    );

    private static final String ITEM_ASSET_TOTAL = "资产总计";
    private static final String ITEM_LIABILITY_TOTAL = "负债合计";

    private static final String ITEM_REVENUE = "营业收入";
    private static final String ITEM_COST = "营业成本";
    private static final String ITEM_NET_PROFIT = "净利润";
    private static final String ITEM_RD_EXPENSE = "研发费用";

    /** 元 转 万元 */
    private static final BigDecimal TEN_THOUSAND = new BigDecimal("10000");

    /** 每个 tableName 对应的文本提取函数 */
    private static final Map<String, Function<ExtractQueryDataRecord, String>> TEXT_EXTRACTORS = Map.of(
            "dib_manage_company_profile", ExtractQueryDataRecord::getCompanyProfileText,
            "dib_director_keyresume", ExtractQueryDataRecord::getResume,
            "dib_manage_business_and_products", ExtractQueryDataRecord::getBusinessAndProductsText,
            "dib_manage_business_circumstance", ExtractQueryDataRecord::getText,
            "dib_company_qualification", ExtractQueryDataRecord::getText,
            "dib_manage_progressiveness_description", ExtractQueryDataRecord::getProgressivenessText,
            "dib_manage_competitive_advantages", ExtractQueryDataRecord::getCompetitivenessText,
            "dib_manage_development_strategy", ExtractQueryDataRecord::getStrategyText,
            "dib_manage_y_industry_analysis", ExtractQueryDataRecord::getText
    );

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadFile(MultipartFile file) {
        fileValidator.validate(java.util.Collections.singletonList(file));
        String token = apiProperties.getDefaultToken();
        String attId = uploadAttachment(file, token);

        ApplicationAttachment record = new ApplicationAttachment();
        record.setAttId(attId);
        record.setFileName(file.getOriginalFilename());
        record.setFileSize(file.getSize());
        record.setCreatedAt(LocalDateTime.now());
        attachmentMapper.insert(record);

        log.info("File uploaded: attId={}, fileName={}", attId, file.getOriginalFilename());
        return attId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submitMaterials(SubmitMaterialsRequest request) {
        validateRequiredParams(request.getCreditCode(), request.getCstId());

        // 从请求体收集文件项，标记 businessType key（"finance"/"business"）
        List<SubmitFileMeta> allItems = new ArrayList<>();
        if (request.getFinanceFiles() != null) {
            for (SubmitMaterialsRequest.SubmitFileItem f : request.getFinanceFiles()) {
                allItems.add(new SubmitFileMeta(f.getAttId(), "finance", f.getReportDate()));
            }
        }
        if (request.getBusinessFile() != null) {
            allItems.add(new SubmitFileMeta(request.getBusinessFile(), "business", null));
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
        record.setCstId(request.getCstId());
        record.setStatus(TaskStatus.UNFINISHED);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        sxdMapper.insert(record);

        try {
            // 构建批量新增参数（从 sxd_att 查文件名/大小，docTypeId 从 financeFiles/businessFile 分类确定）
            List<DocBatchAddItem> batchItems = buildBatchAddItems(allItems);
            ExternalResponse batchResponse = batchAddDocs(batchItems, token);

            DocBatchAddData batchData = batchResponse.getDataAs(DocBatchAddData.class);
            if (batchData.getInvalidDocNames() != null
                    && !batchData.getInvalidDocNames().isEmpty()) {
                log.warn("Some documents failed to add for taskId={}: {}",
                        batchTaskId, batchData.getInvalidDocNames());
            }

            // 通过 attId 匹配请求体中的 reportDate 和 businessType（已回填为 docTypeId），创建 DocEntry 并插入
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

            // 提交成功后删除 sxd_att 中对应的附件记录
            for (SubmitFileMeta item : allItems) {
                attachmentMapper.delete(
                        new LambdaQueryWrapper<ApplicationAttachment>()
                                .eq(ApplicationAttachment::getAttId, item.attId));
            }

            log.info("Application record submitted: taskId={}, creditCode={}, docCount={}",
                    batchTaskId, record.getCreditCode(), batchData.getDocList().size());

            return batchTaskId;

        } catch (BusinessException e) {
            log.warn("Submission failed for taskId={}: {}", batchTaskId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error submitting taskId={}", batchTaskId, e);
            throw new BusinessException("BATCH_ADD_FAILED",
                    "资料批量新增异常：" + e.getMessage());
        }
    }

    private void validateRequiredParams(String creditCode, String cstId) {
        if (!StringUtils.hasText(creditCode)) {
            throw new BusinessException("PARAM_MISSING", "统一社会信用代码不能为空");
        }
        if (!StringUtils.hasText(cstId)) {
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
            if (respBody == null) {
                throw new BusinessException("ATTACH_UPLOAD_FAILED", "附件上传失败：未知错误");
            }
            if (!respBody.isSuccess()) {
                throw new BusinessException("ATTACH_UPLOAD_FAILED", respBody.getMessage());
            }
            return (String) respBody.getData();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Attachment upload failed for file: {}", file.getOriginalFilename(), e);
            throw new BusinessException("ATTACH_UPLOAD_FAILED", e.getMessage());
        }
    }

    /**
     * 根据请求中的文件项列表构建批量新增请求参数。
     * fileName/fileSize 从 sxd_att 表查询，docTypeId 根据 financeFiles/businessFile 分类从配置获取。
     */
    private List<DocBatchAddItem> buildBatchAddItems(List<SubmitFileMeta> items) {
        List<DocBatchAddItem> result = new ArrayList<>();
        Map<String, Long> docTypeMap = apiProperties.getDocType();
        Long dirId = apiProperties.getDirId();
        Long projectId = apiProperties.getProjectId();
        for (SubmitFileMeta item : items) {
            // 根据 businessType key ("finance"/"business") 查找对应的 docTypeId
            Long docTypeId = docTypeMap.get(item.businessType);
            if (docTypeId == null) {
                throw new BusinessException("INVALID_BUSINESS_TYPE",
                        "未知的业务类型：" + item.businessType);
            }
            // 从 sxd_att 查询文件元信息
            ApplicationAttachment att = attachmentMapper.selectOne(
                    new LambdaQueryWrapper<ApplicationAttachment>()
                            .eq(ApplicationAttachment::getAttId, item.attId));
            if (att == null) {
                throw new BusinessException("ATTACH_NOT_FOUND",
                        "附件 " + item.attId + " 不存在，请重新上传");
            }
            // 回填 businessType 为 docTypeId 值，便于下游 DocEntry 使用
            item.businessType = String.valueOf(docTypeId);
            // 附加 attId 后 6 位确保 docName 全局唯一（外部 API 要求 docName 不重复）
            String uniqueDocName = makeUniqueDocName(att.getFileName(), item.attId);
            result.add(DocBatchAddItem.builder()
                    .attId(item.attId)
                    .dirId(dirId)
                    .docName(uniqueDocName)
                    .docSize(att.getFileSize() / 1024)   // byte → KB
                    .docTypeId(docTypeId)
                    .extraInfo("{}")
                    .projectId(projectId)
                    .reportDate(item.reportDate)
                    .build());
        }
        return result;
    }

    /**
     * 在文件名末尾附加 attId 后 6 位，确保 docName 全局唯一。
     * 例如：财报.pdf → 财报_a3f2c1.pdf
     */
    private static String makeUniqueDocName(String fileName, String attId) {
        String suffix = attId.substring(Math.max(0, attId.length() - 6));
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            return fileName.substring(0, dot) + "_" + suffix + fileName.substring(dot);
        }
        return fileName + "_" + suffix;
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
                throw new BusinessException("BATCH_ADD_FAILED", "资料批量新增失败：未知错误");
            }
            if (!respBody.isSuccess() || respBody.getData() == null) {
                throw new BusinessException("BATCH_ADD_FAILED", respBody.getMessage());
            }
            return respBody;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Batch add documents failed", e);
            throw new BusinessException("BATCH_ADD_FAILED", e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmControllerName(ConfirmControllerRequest request) {
        if (request == null || !StringUtils.hasText(request.getTaskId())) {
            throw new BusinessException("PARAM_MISSING", "任务 ID 不能为空");
        }
        if (!StringUtils.hasText(request.getActCntlrNm())) {
            throw new BusinessException("PARAM_MISSING", "实际控制人姓名不能为空");
        }

        ApplicationRecord record = sxdMapper.selectById(request.getTaskId());
        if (record == null) {
            throw new BusinessException("TASK_NOT_FOUND",
                    "任务 [" + request.getTaskId() + "] 不存在");
        }

        record.setActCntlrNm(request.getActCntlrNm());
        record.setUpdatedAt(LocalDateTime.now());
        sxdMapper.updateById(record);

        log.info("Controller name confirmed: taskId={}, actCntlrNm={}",
                request.getTaskId(), request.getActCntlrNm());
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

    @Override
    public ExtractStatusResponse queryExtractStatus(String taskId) {
        // 查询该任务下的所有文档
        List<DocEntry> docEntries = docEntryMapper.selectList(
                new LambdaQueryWrapper<DocEntry>()
                        .eq(DocEntry::getTaskId, taskId));

        if (docEntries == null || docEntries.isEmpty()) {
            throw new BusinessException("DOC_NOT_FOUND",
                    "任务 [" + taskId + "] 下未找到文档记录");
        }

        String token = apiProperties.getDefaultToken();
        String detailUrlBase = apiProperties.getDocDetailUrl();
        List<String> pendingDocNames = new ArrayList<>();

        for (DocEntry entry : docEntries) {
            DocDetailData detail = getDocDetail(detailUrlBase + "/" + entry.getDocId(), token);
            String state = detail.getExtractState();
            // U=待执行, I=执行中 — 视为未完成
            if ("U".equals(state) || "I".equals(state)) {
                pendingDocNames.add(detail.getDocName());
            }
        }

        boolean completed = pendingDocNames.isEmpty();
        return new ExtractStatusResponse(completed, pendingDocNames);
    }

    @Override
    public ExtractDataResponse queryExtractData(String taskId) {
        // 查商业计划书类型（businessType = docType.business 的值）的文档
        String businessDocType = String.valueOf(apiProperties.getDocType().get("business"));
        List<DocEntry> docEntries = docEntryMapper.selectList(
                new LambdaQueryWrapper<DocEntry>()
                        .eq(DocEntry::getTaskId, taskId)
                        .eq(DocEntry::getBusinessType, businessDocType));

        if (docEntries == null || docEntries.isEmpty()) {
            throw new BusinessException("DOC_NOT_FOUND",
                    "任务 [" + taskId + "] 下未找到商业计划书文档");
        }

        String token = apiProperties.getDefaultToken();
        // 收集每个 tableName 对应的文本列表（可能有多个 doc，多个记录）
        Map<String, List<String>> tableTexts = new LinkedHashMap<>();

        for (DocEntry entry : docEntries) {
            Long docId;
            try {
                docId = Long.parseLong(entry.getDocId());
            } catch (NumberFormatException e) {
                log.warn("Invalid docId format: {}", entry.getDocId());
                throw new BusinessException("DOC_DATA_FAILED",
                        "文档 ID 格式无效：" + entry.getDocId());
            }

            for (String tableName : BUSINESS_PLAN_TABLES) {
                List<String> texts = queryExtractDataByTable(docId, tableName, token);
                tableTexts.computeIfAbsent(tableName, k -> new ArrayList<>()).addAll(texts);
            }
        }

        // 按 BUSINESS_PLAN_TABLES 顺序构建响应
        List<ExtractDataResponse.ExtractDataItem> extractData = new ArrayList<>();
        for (String tableName : BUSINESS_PLAN_TABLES) {
            List<String> texts = tableTexts.getOrDefault(tableName, Collections.emptyList());
            String mergedText = String.join("\n", texts);
            extractData.add(new ExtractDataResponse.ExtractDataItem(tableName, mergedText));
        }

        return new ExtractDataResponse(extractData);
    }

    @Override
    public byte[] exportBusinessExtractData(String taskId) {
        String businessDocType = String.valueOf(apiProperties.getDocType().get("business"));
        List<DocEntry> entries = docEntryMapper.selectList(
                new LambdaQueryWrapper<DocEntry>()
                        .eq(DocEntry::getTaskId, taskId)
                        .eq(DocEntry::getBusinessType, businessDocType));

        if (entries == null || entries.isEmpty()) {
            throw new BusinessException("DOC_NOT_FOUND",
                    "任务 [" + taskId + "] 下未找到商业计划书文档");
        }

        // 商业计划书仅单个文件，取第一个文档
        return downloadExportFile(entries.get(0).getDocId());
    }

    @Override
    public byte[] exportFinanceExtractData(String taskId) {
        String financeDocType = String.valueOf(apiProperties.getDocType().get("finance"));
        List<DocEntry> entries = docEntryMapper.selectList(
                new LambdaQueryWrapper<DocEntry>()
                        .eq(DocEntry::getTaskId, taskId)
                        .eq(DocEntry::getBusinessType, financeDocType));

        if (entries == null || entries.isEmpty()) {
            throw new BusinessException("DOC_NOT_FOUND",
                    "任务 [" + taskId + "] 下未找到财务报表文档");
        }

        // 逐个下载每个财务报表的 xlsx 并打包为 zip，命名格式：财务报表_{reportDate}.xlsx
        Map<String, Integer> dateCounter = new HashMap<>();
        List<AbstractMap.SimpleEntry<String, byte[]>> fileEntries = new ArrayList<>();
        for (DocEntry entry : entries) {
            byte[] data = downloadExportFile(entry.getDocId());
            // 处理空日期和重复日期
            String baseName = entry.getReportDate() != null && !entry.getReportDate().isEmpty()
                    ? entry.getReportDate() : "未知日期";
            int count = dateCounter.merge(baseName, 1, Integer::sum);
            String fileName = count == 1
                    ? "财务报表_" + baseName + ".xlsx"
                    : "财务报表_" + baseName + "_" + count + ".xlsx";
            fileEntries.add(new AbstractMap.SimpleEntry<>(fileName, data));
        }

        return createFinanceZip(fileEntries);
    }

    /**
     * 调用外部导出资料接口，下载单个文档的 xlsx 文件字节流。
     */
    private byte[] downloadExportFile(String docId) {
        String url = apiProperties.getDocExportDataUrl() + "/" + docId;
        try {
            HttpHeaders headers = new HttpHeaders();
            if (StringUtils.hasText(apiProperties.getDefaultToken())) {
                headers.set("c1-token", apiProperties.getDefaultToken());
            }
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, byte[].class);

            byte[] body = response.getBody();
            if (body == null || body.length == 0) {
                throw new BusinessException("DOC_EXPORT_FAILED",
                        "文档 " + docId + " 导出数据为空");
            }
            log.info("Exported doc data: docId={}, size={} bytes", docId, body.length);
            return body;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to export doc data for docId={}", docId, e);
            throw new BusinessException("DOC_EXPORT_FAILED",
                    "文档 " + docId + " 导出失败：" + e.getMessage());
        }
    }

    /**
     * 将多个 xlsx 文件打包成 zip 压缩包。
     *
     * @param fileEntries entry 列表，每个 entry 的 key 为文件名，value 为文件字节数据
     */
    private byte[] createFinanceZip(List<AbstractMap.SimpleEntry<String, byte[]>> fileEntries) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (AbstractMap.SimpleEntry<String, byte[]> fileEntry : fileEntries) {
                String entryName = fileEntry.getKey();
                byte[] data = fileEntry.getValue();
                ZipEntry zipEntry = new ZipEntry(entryName);
                zos.putNextEntry(zipEntry);
                zos.write(data);
                zos.closeEntry();
            }
        } catch (IOException e) {
            log.error("Failed to create zip archive", e);
            throw new BusinessException("ZIP_CREATE_FAILED",
                    "财务报表压缩包创建失败：" + e.getMessage());
        }
        return baos.toByteArray();
    }

    /**
     * 调用外部提取数据查询 API，返回该 tableName 下的文本列表。
     */
    private List<String> queryExtractDataByTable(Long docId, String tableName, String token) {
        ExtractQueryDataRequest request = new ExtractQueryDataRequest(docId, tableName);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(token)) {
                headers.set("c1-token", token);
            }

            HttpEntity<ExtractQueryDataRequest> requestEntity = new HttpEntity<>(request, headers);
            ResponseEntity<ExternalResponse> response = restTemplate.exchange(
                    apiProperties.getDocQueryDataUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    ExternalResponse.class);

            ExternalResponse respBody = response.getBody();
            if (respBody == null) {
                throw new BusinessException("DOC_QUERY_FAILED",
                        "提取数据查询失败：未知错误");
            }
            if (!respBody.isSuccess() || respBody.getData() == null) {
                throw new BusinessException("DOC_QUERY_FAILED",
                        "提取数据查询失败：" + respBody.getMessage());
            }

            List<ExtractQueryDataRecord> records = respBody.getDataAsList(ExtractQueryDataRecord.class);
            if (records == null || records.isEmpty()) {
                return Collections.emptyList();
            }

            Function<ExtractQueryDataRecord, String> extractor = TEXT_EXTRACTORS.get(tableName);
            if (extractor == null) {
                log.warn("No text extractor for tableName: {}", tableName);
                return Collections.emptyList();
            }

            return records.stream()
                    .map(extractor)
                    .filter(Objects::nonNull)
                    .filter(s -> !s.trim().isEmpty())
                    .collect(Collectors.toList());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to query extract data for docId={}, tableName={}", docId, tableName, e);
            throw new BusinessException("DOC_QUERY_FAILED",
                    "提取数据查询异常：" + e.getMessage());
        }
    }

    private DocDetailData getDocDetail(String url, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (StringUtils.hasText(token)) {
                headers.set("c1-token", token);
            }
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<ExternalResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, ExternalResponse.class);
            ExternalResponse respBody = response.getBody();
            if (respBody == null) {
                throw new BusinessException("DOC_DETAIL_FAILED", "资料详情查询失败：未知错误");
            }
            if (!respBody.isSuccess() || respBody.getData() == null) {
                throw new BusinessException("DOC_DETAIL_FAILED", respBody.getMessage());
            }
            return respBody.getDataAs(DocDetailData.class);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to query doc detail for url: {}", url, e);
            throw new BusinessException("DOC_DETAIL_FAILED", "资料详情查询异常：" + e.getMessage());
        }
    }

    private String generateTaskId() {
        UUID uuid = UUID.randomUUID();
        return "TASK-" + String.format("%016x%016x",
                uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    // ========== 报告生成功能 ==========

    @Override
    public byte[] generateReport(String taskId, String cstId) {
        // ============ 校验任务存在 ============
        ApplicationRecord record = sxdMapper.selectById(taskId);
        if (record == null) {
            throw new BusinessException("TASK_NOT_FOUND",
                    "任务 [" + taskId + "] 不存在");
        }

        // ============ 查询企业信息 ============
        CustomerProfile customerProfile = null;
        if (cstId != null && !cstId.isEmpty()) {
            try {
                customerProfile = customerService.getCustomerProfile(cstId);
            } catch (BusinessException e) {
                log.warn("Customer profile not found for cstId={}: {}", cstId, e.getMessage());
            }
        }

        String financeDocType = String.valueOf(apiProperties.getDocType().get("finance"));
        List<DocEntry> entries = docEntryMapper.selectList(
                new LambdaQueryWrapper<DocEntry>()
                        .eq(DocEntry::getTaskId, taskId)
                        .eq(DocEntry::getBusinessType, financeDocType));

        if (entries == null || entries.isEmpty()) {
            throw new BusinessException("DOC_NOT_FOUND",
                    "任务 [" + taskId + "] 下未找到财务报表文档");
        }

        String token = apiProperties.getDefaultToken();

        // ============ 资产负债表处理 ============
        Map<String, Map<String, BigDecimal>> bsItemDateValues = new LinkedHashMap<>();
        List<String> bsDateColumns = new ArrayList<>();

        for (DocEntry entry : entries) {
            Long docId;
            try {
                docId = Long.parseLong(entry.getDocId());
            } catch (NumberFormatException e) {
                log.warn("Invalid docId format, skipping: {}", entry.getDocId());
                continue;
            }

            String tableName;
            List<BalanceSheetRecord> records;
            try {
                tableName = determineBalanceSheetTable(docId, token);
                records = queryBalanceSheetData(docId, tableName, token);
            } catch (BusinessException e) {
                log.warn("Query failed for docId={} balance sheet: {}, outputting empty column", docId, e.getMessage());
                records = null;
            }
            if (records == null || records.isEmpty()) {
                log.warn("No balance sheet data for docId={}, outputting empty column", docId);
            }

            String reportDate = (records != null && !records.isEmpty()) ? extractReportDateFromRecords(records) : null;
            if (reportDate == null || reportDate.isEmpty()) {
                reportDate = entry.getReportDate();
            }
            if (reportDate == null || reportDate.isEmpty()) {
                log.warn("No report date for docId={}, skipping", docId);
                continue;
            }

            String dateCol = formatDateColumn(reportDate);
            if (!bsDateColumns.contains(dateCol)) {
                bsDateColumns.add(dateCol);
            }

            if (records != null) {
                for (BalanceSheetRecord r : records) {
                    String itemName = r.getItem();
                    if (itemName != null && BALANCE_SHEET_KEY_ITEMS.contains(itemName)
                            && r.getCurrentAmount() != null) {
                        bsItemDateValues.computeIfAbsent(itemName, k -> new LinkedHashMap<>())
                                .put(dateCol, r.getCurrentAmount());
                    }
                }
            }
        }

        if (bsDateColumns.isEmpty()) {
            throw new BusinessException("NO_BALANCE_SHEET_DATA",
                    "任务 [" + taskId + "] 下未找到有效的资产负债表数据");
        }

        // ============ 利润表处理 ============
        Map<String, Map<String, BigDecimal>> psItemDateValues = new LinkedHashMap<>();
        List<String> psDateColumns = new ArrayList<>();

        for (DocEntry entry : entries) {
            Long docId;
            try {
                docId = Long.parseLong(entry.getDocId());
            } catch (NumberFormatException e) {
                log.warn("Invalid docId format, skipping: {}", entry.getDocId());
                continue;
            }

            String tableName;
            List<BalanceSheetRecord> records;
            try {
                tableName = determineProfitSheetTable(docId, token);
                records = queryBalanceSheetData(docId, tableName, token);
            } catch (BusinessException e) {
                log.warn("Query failed for docId={} profit sheet: {}, outputting empty column", docId, e.getMessage());
                records = null;
            }
            if (records == null || records.isEmpty()) {
                log.warn("No profit sheet data for docId={}, outputting empty column", docId);
            }

            String reportDate = (records != null && !records.isEmpty()) ? extractReportDateFromRecords(records) : null;
            if (reportDate == null || reportDate.isEmpty()) {
                reportDate = entry.getReportDate();
            }
            if (reportDate == null || reportDate.isEmpty()) {
                log.warn("No report date for docId={}, skipping", docId);
                continue;
            }

            String dateCol = formatDateColumn(reportDate);
            if (!psDateColumns.contains(dateCol)) {
                psDateColumns.add(dateCol);
            }

            if (records != null) {
                for (String itemName : PROFIT_SHEET_KEY_ITEMS) {
                    BigDecimal value = findProfitItemValue(records, itemName);
                    if (value != null) {
                        psItemDateValues.computeIfAbsent(itemName, k -> new LinkedHashMap<>())
                                .put(dateCol, value);
                    }
                }
            }
        }

        // ============ 更新任务状态 + 清理文档记录 ============
        docEntryMapper.delete(new LambdaQueryWrapper<DocEntry>()
                .eq(DocEntry::getTaskId, taskId));
        record.setStatus(TaskStatus.COMPLETED);
        record.setUpdatedAt(LocalDateTime.now());
        sxdMapper.updateById(record);

        // ============ 生成 Word 文档 ============
        return createWordDocument(customerProfile, bsItemDateValues, bsDateColumns,
                psItemDateValues, psDateColumns);
    }

    /**
     * 查询文档的表格提取状态列表。
     */
    private List<DocTableStateRecord> queryDocTableStates(Long docId, String token) {
        String url = apiProperties.getDocTableExtractStateUrl() + "/" + docId;
        try {
            HttpHeaders headers = new HttpHeaders();
            if (StringUtils.hasText(token)) {
                headers.set("c1-token", token);
            }
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<ExternalResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, ExternalResponse.class);
            ExternalResponse respBody = response.getBody();
            if (respBody == null || !respBody.isSuccess() || respBody.getData() == null) {
                throw new BusinessException("TABLE_STATE_FAILED",
                        "文档 " + docId + " 表格提取状态查询失败");
            }
            return respBody.getDataAsList(DocTableStateRecord.class);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to query table extract state for docId={}", docId, e);
            throw new BusinessException("TABLE_STATE_FAILED",
                    "表格提取状态查询异常：" + e.getMessage());
        }
    }

    /**
     * 通过表格提取状态和审计报告附注判断使用合并还是母公司资产负债表。
     * <p>
     * ① 有审计报告附注 → 查询"财务报表口径"：1-单一（母公司表）/ 2-合并（合并表）
     * ② 无审计报告附注 → dib_fin_balance 和 dib_fin_balance_parent 哪个状态为Y用哪个，都Y或都F默认合并表
     */
    private String determineBalanceSheetTable(Long docId, String token) {
        List<DocTableStateRecord> states = queryDocTableStates(docId, token);

        DocTableStateRecord auditNote = null;
        DocTableStateRecord balance = null;
        DocTableStateRecord balanceParent = null;

        for (DocTableStateRecord state : states) {
            if ("dib_intervening_y_auditreport_jh".equals(state.getTableName())) {
                auditNote = state;
            } else if ("dib_fin_balance".equals(state.getTableName())) {
                balance = state;
            } else if ("dib_fin_balance_parent".equals(state.getTableName())) {
                balanceParent = state;
            }
        }

        // ① 审计报告附注已提取 → 查询"财务报表口径"
        if (auditNote != null && "Y".equals(auditNote.getExtractState())) {
            String scope = queryFinanceReportScope(docId, token);
            if ("1".equals(scope)) {
                log.info("Doc {} 财务报表口径=单一，使用母公司资产负债表", docId);
                return "dib_fin_balance_parent";
            } else if ("2".equals(scope)) {
                log.info("Doc {} 财务报表口径=合并，使用合并资产负债表", docId);
                return "dib_fin_balance";
            }
            // scope 查询失败，回退到状态判断
            log.warn("Doc {} 无法确定财务报表口径（scope={}），回退到状态判断", docId, scope);
        }

        // ② 无审计报告附注（或口径查询失败）→ 查看状态
        boolean balanceY = balance != null && "Y".equals(balance.getExtractState());
        boolean parentY = balanceParent != null && "Y".equals(balanceParent.getExtractState());

        if (balanceY && !parentY) {
            log.info("Doc {} 合并资产负债表状态为Y，使用合并表", docId);
            return "dib_fin_balance";
        } else if (!balanceY && parentY) {
            log.info("Doc {} 母公司资产负债表状态为Y，使用母公司表", docId);
            return "dib_fin_balance_parent";
        } else {
            // 状态都为Y或都非Y → 默认合并资产负债表
            log.info("Doc {} 默认使用合并资产负债表（balanceY={}, parentY={})", docId, balanceY, parentY);
            return "dib_fin_balance";
        }
    }

    /**
     * 通过表格提取状态和审计报告附注判断使用合并还是母公司利润表。
     * <p>
     * ① 有审计报告附注 → 查询"财务报表口径"：1-单一（母公司表）/ 2-合并（合并表）
     * ② 无审计报告附注 → dib_fin_profit_statement 和 dib_fin_profit_statement_parent
     *    哪个状态为Y用哪个，都Y或都F默认合并利润表
     */
    private String determineProfitSheetTable(Long docId, String token) {
        List<DocTableStateRecord> states = queryDocTableStates(docId, token);

        DocTableStateRecord auditNote = null;
        DocTableStateRecord profit = null;
        DocTableStateRecord profitParent = null;

        for (DocTableStateRecord state : states) {
            if ("dib_intervening_y_auditreport_jh".equals(state.getTableName())) {
                auditNote = state;
            } else if ("dib_fin_profit_statement".equals(state.getTableName())) {
                profit = state;
            } else if ("dib_fin_profit_statement_parent".equals(state.getTableName())) {
                profitParent = state;
            }
        }

        // ① 审计报告附注已提取 → 查询"财务报表口径"
        if (auditNote != null && "Y".equals(auditNote.getExtractState())) {
            String scope = queryFinanceReportScope(docId, token);
            if ("1".equals(scope)) {
                log.info("Doc {} 财务报表口径=单一，使用母公司利润表", docId);
                return "dib_fin_profit_statement_parent";
            } else if ("2".equals(scope)) {
                log.info("Doc {} 财务报表口径=合并，使用合并利润表", docId);
                return "dib_fin_profit_statement";
            }
            log.warn("Doc {} 无法确定财务报表口径（scope={}），回退到状态判断", docId, scope);
        }

        // ② 无审计报告附注（或口径查询失败）→ 查看状态
        boolean profitY = profit != null && "Y".equals(profit.getExtractState());
        boolean parentY = profitParent != null && "Y".equals(profitParent.getExtractState());

        if (profitY && !parentY) {
            log.info("Doc {} 合并利润表状态为Y，使用合并表", docId);
            return "dib_fin_profit_statement";
        } else if (!profitY && parentY) {
            log.info("Doc {} 母公司利润表状态为Y，使用母公司表", docId);
            return "dib_fin_profit_statement_parent";
        } else {
            log.info("Doc {} 默认使用合并利润表（profitY={}, parentY={})", docId, profitY, parentY);
            return "dib_fin_profit_statement";
        }
    }

    /**
     * 从审计报告附注表（dib_intervening_y_auditreport_jh）中查询"财务报表口径"的值。
     *
     * @return "1"(单一) 或 "2"(合并)，查询失败返回 null
     */
    private String queryFinanceReportScope(Long docId, String token) {
        ExtractQueryDataRequest request = new ExtractQueryDataRequest(docId,
                "dib_intervening_y_auditreport_jh");
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(token)) {
                headers.set("c1-token", token);
            }
            HttpEntity<ExtractQueryDataRequest> requestEntity = new HttpEntity<>(request, headers);
            ResponseEntity<ExternalResponse> response = restTemplate.exchange(
                    apiProperties.getDocQueryDataUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    ExternalResponse.class);
            ExternalResponse respBody = response.getBody();
            if (respBody == null || !respBody.isSuccess() || respBody.getData() == null) {
                log.warn("Failed to query audit report for docId={}", docId);
                return null;
            }
            List<AuditReportItem> items = respBody.getDataAsList(AuditReportItem.class);
            if (items == null) return null;
            for (AuditReportItem item : items) {
                if ("财务报表口径".equals(item.getItem())) {
                    return item.getItemValue();
                }
            }
        } catch (Exception e) {
            log.warn("Exception querying audit report scope for docId={}: {}", docId, e.getMessage());
        }
        return null;
    }

    /**
     * 查询资产负债表的提取数据（调用外部 queryData 接口）。
     */
    private List<BalanceSheetRecord> queryBalanceSheetData(Long docId, String tableName, String token) {
        ExtractQueryDataRequest request = new ExtractQueryDataRequest(docId, tableName);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(token)) {
                headers.set("c1-token", token);
            }
            HttpEntity<ExtractQueryDataRequest> requestEntity = new HttpEntity<>(request, headers);
            ResponseEntity<ExternalResponse> response = restTemplate.exchange(
                    apiProperties.getDocQueryDataUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    ExternalResponse.class);
            ExternalResponse respBody = response.getBody();
            if (respBody == null || !respBody.isSuccess() || respBody.getData() == null) {
                throw new BusinessException("DOC_QUERY_FAILED",
                        "资产负债表数据查询失败：" + (respBody != null ? respBody.getMessage() : "未知错误"));
            }
            return respBody.getDataAsList(BalanceSheetRecord.class);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to query balance sheet data for docId={}, tableName={}", docId, tableName, e);
            throw new BusinessException("DOC_QUERY_FAILED",
                    "资产负债表数据查询异常：" + e.getMessage());
        }
    }

    /**
     * 从查询结果中提取第一个非空的 report_date。
     */
    private String extractReportDateFromRecords(List<BalanceSheetRecord> records) {
        if (records == null) return null;
        for (BalanceSheetRecord record : records) {
            if (record.getReportDate() != null && !record.getReportDate().isEmpty()) {
                return record.getReportDate();
            }
        }
        return null;
    }

    /**
     * 将 YYYY-MM-DD 格式的日期转换为列标题。
     * 12-31 → "2024年"（年末报告），否则 → "2025年6月"
     */
    private String formatDateColumn(String reportDate) {
        if (reportDate == null || reportDate.isEmpty()) return "未知日期";
        String[] parts = reportDate.split("-");
        if (parts.length < 2) return reportDate;
        String year = parts[0];
        try {
            int month = Integer.parseInt(parts[1]);
            if (parts.length >= 3 && "12".equals(parts[1]) && "31".equals(parts[2])) {
                return year + "年";
            }
            return year + "年" + month + "月";
        } catch (NumberFormatException e) {
            return year + "年";
        }
    }

    /**
     * 从日期列标题中提取年份。如 "2024年" → "2024"，"2025年9月" → "2025"
     */
    private String extractYearFromColumn(String column) {
        if (column == null) return "";
        int idx = column.indexOf("年");
        if (idx > 0) {
            return column.substring(0, idx);
        }
        return column;
    }

    /**
     * 将金额从 元 格式化为 万元 显示（千分位，2位小数）。
     * null → "-"
     */
    private String formatAmount(BigDecimal valueYuan) {
        if (valueYuan == null) return "-";
        BigDecimal valueWan = valueYuan.divide(TEN_THOUSAND, 2, RoundingMode.HALF_UP);
        return String.format("%,.2f", valueWan);
    }

    /**
     * 计算增长率并格式化。
     * (current - previous) / |previous| * 100，保留2位小数加百分号。
     * previous=0 → "9999999"；任一为 null → "-"
     */
    private String formatGrowthRate(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null) return "-";
        if (previous.compareTo(BigDecimal.ZERO) == 0) return "-";
        BigDecimal rate = current.subtract(previous)
                .divide(previous.abs(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        return String.format("%.2f%%", rate);
    }

    /**
     * 计算资产负债率并格式化为百分比。负债合计 / 资产总计 * 100%
     */
    private String formatLiabilityRatio(BigDecimal assetTotal, BigDecimal liabilityTotal) {
        if (assetTotal == null || liabilityTotal == null) return "-";
        if (assetTotal.compareTo(BigDecimal.ZERO) == 0) return "-";
        BigDecimal ratio = liabilityTotal.divide(assetTotal, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        return String.format("%.2f%%", ratio);
    }

    /**
     * 计算资产负债率的数值（用于增长率计算）。null 表示无法计算。
     */
    private BigDecimal calcLiabilityRatio(BigDecimal assetTotal, BigDecimal liabilityTotal) {
        if (assetTotal == null || liabilityTotal == null || assetTotal.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return liabilityTotal.divide(assetTotal, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    // ========== 利润表辅助方法 ==========

    /**
     * 从利润表记录中按 item_standard 精确匹配指定科目的 current_amount。
     * 如"营业收入"匹配 item_standard="营业总收入"。
     */
    private BigDecimal findProfitItemValue(List<BalanceSheetRecord> records, String displayName) {
        List<String> keys = PROFIT_ITEM_SEARCH_KEYS.get(displayName);
        if (keys == null || records == null) return null;

        for (BalanceSheetRecord r : records) {
            if (r.getItemStandard() != null && keys.contains(r.getItemStandard())) {
                return r.getCurrentAmount();
            }
        }
        return null;
    }

    /**
     * 计算比率并格式化为百分比。numerator / denominator × 100%
     * 任一参数为 null 或 denominator=0 时返回 "-"
     */
    private String formatRatio(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null) return "-";
        if (denominator.compareTo(BigDecimal.ZERO) == 0) return "-";
        BigDecimal ratio = numerator.divide(denominator, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        return String.format("%.2f%%", ratio);
    }

    /**
     * 构建利润表关键科目的 markdown 表格。
     * <p>
     * 包含 9 个基本科目（营业收入 ~ 净利润）+ 3 个计算比例行（毛利润率、净利润率、研发费用占比）。
     * 增长率规则同资产负债表关键科目：取最后两个年末（12-31）列计算同比。
/** 提交时从请求体解析的文件项（attId + businessType key + reportDate），businessType 在 buildBatchAddItems 中回填为 docTypeId */


    // ========== Word 文档生成（模板模式） ==========

    /**
     * 打开模板文档，替换 {{占位符}} 为实际数据，返回 Word 文档字节。
     * <p>
     * 模板中 {{{{field_name}}}} 占位符会被替换为客户信息字段值，
     * {{{{资产负债表关键科目}}}} 和 {{{{利润表关键科目}}}} 会被替换为对应的数据表格。
     */
    private byte[] createWordDocument(CustomerProfile profile,
                                       Map<String, Map<String, BigDecimal>> bsItemDateValues, List<String> bsDateColumns,
                                       Map<String, Map<String, BigDecimal>> psItemDateValues, List<String> psDateColumns) {
        String templatePath = apiProperties.getReportTemplatePath();
        Resource templateResource = resourceLoader.getResource(templatePath);
        if (!templateResource.exists()) {
            throw new BusinessException("TEMPLATE_NOT_FOUND",
                    "报告模板文件不存在: " + templatePath);
        }

        try (InputStream is = templateResource.getInputStream();
             XWPFDocument doc = new XWPFDocument(is);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // 1. 替换客户基本信息占位符 {{cst_nm}}、{{credit_code}} 等
            replaceProfilePlaceholders(doc, profile);

            // 2. 替换资产负债表关键科目占位符 -> 插入表格
            if (!bsDateColumns.isEmpty()) {
                replacePlaceholderWithTable(doc, "{{资产负债表关键科目}}",
                        table -> fillBalanceSheetTable(table, bsItemDateValues, bsDateColumns));
            }

            // 3. 替换利润表关键科目占位符 -> 插入表格
            if (!psDateColumns.isEmpty()) {
                replacePlaceholderWithTable(doc, "{{利润表关键科目}}",
                        table -> fillProfitSheetTable(table, psItemDateValues, psDateColumns));
            }

            doc.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to create Word document from template", e);
            throw new BusinessException("DOC_CREATE_FAILED", "Word 文档生成失败：" + e.getMessage());
        }
    }

    /**
     * 替换文档段落中 {{{{field_name}}}} 格式的占位符为实际客户信息字段值。
     */
    private void replaceProfilePlaceholders(XWPFDocument doc, CustomerProfile profile) {
        for (XWPFParagraph para : doc.getParagraphs()) {
            for (XWPFRun run : para.getRuns()) {
                String text = run.getText(0);
                if (text == null || !text.contains("{{")) continue;

                String replaced = text;
                boolean changed = false;
                for (Map.Entry<String, Function<CustomerProfile, String>> entry : PROFILE_FIELD_GETTERS.entrySet()) {
                    String placeholder = "{{" + entry.getKey() + "}}";
                    if (replaced.contains(placeholder)) {
                        String value = profile != null ? entry.getValue().apply(profile) : "-";
                        replaced = replaced.replace(placeholder, value != null ? value : "-");
                        changed = true;
                    }
                }
                if (changed) {
                    run.setText(replaced, 0);
                }
            }
        }
    }

    /**
     * 在文档中查找包含指定占位符文本的段落，在其位置插入一个新表格，
     * 然后移除该占位符段落。通过 XmlCursor 在占位符段落之前插入表格元素。
     */
    private void replacePlaceholderWithTable(XWPFDocument doc, String placeholder,
                                              Consumer<XWPFTable> tableFiller) {
        List<IBodyElement> bodyElements = doc.getBodyElements();
        for (int i = 0; i < bodyElements.size(); i++) {
            IBodyElement elem = bodyElements.get(i);
            if (elem instanceof XWPFParagraph) {
                XWPFParagraph para = (XWPFParagraph) elem;
                if (para.getText().contains(placeholder)) {
                    XmlCursor cursor = para.getCTP().newCursor();
                    XWPFTable table = doc.insertNewTbl(cursor);
                    cursor.dispose();

                    tableFiller.accept(table);

                    doc.removeBodyElement(i + 1);
                    return;
                }
            }
        }
        log.warn("Placeholder not found in template: {}", placeholder);
    }

    /**
     * 向已创建的空白表格填充资产负债表关键科目数据。
     */
    private void fillBalanceSheetTable(XWPFTable table,
                                        Map<String, Map<String, BigDecimal>> itemDateValues,
                                        List<String> dateColumns) {
        List<String[]> growthCols = buildGrowthCols(dateColumns);
        int colCount = 1 + dateColumns.size() + growthCols.size();
        int rowCount = 1 + BALANCE_SHEET_KEY_ITEMS.size() + 1;

        for (int r = 0; r < rowCount; r++) {
            ensureCellCount(table.createRow(), colCount);
        }

        // 表头行
        XWPFTableRow headerRow = table.getRow(0);
        setCellText(headerRow.getCell(0), "项目", true, null);
        for (int i = 0; i < dateColumns.size(); i++) {
            setCellText(headerRow.getCell(1 + i), dateColumns.get(i), true, null);
        }
        for (int i = 0; i < growthCols.size(); i++) {
            setCellText(headerRow.getCell(1 + dateColumns.size() + i), growthCols.get(i)[0], true, null);
        }

        // 关键科目数据行
        for (int r = 0; r < BALANCE_SHEET_KEY_ITEMS.size(); r++) {
            String item = BALANCE_SHEET_KEY_ITEMS.get(r);
            XWPFTableRow row = table.getRow(1 + r);
            setCellText(row.getCell(0), item, false, null);

            Map<String, BigDecimal> values = itemDateValues.get(item);
            for (int c = 0; c < dateColumns.size(); c++) {
                BigDecimal val = values != null ? values.get(dateColumns.get(c)) : null;
                setCellText(row.getCell(1 + c), formatAmount(val), false, null);
            }
            for (int g = 0; g < growthCols.size(); g++) {
                String[] ginfo = growthCols.get(g);
                BigDecimal curVal = values != null ? values.get(ginfo[1]) : null;
                BigDecimal prevVal = values != null ? values.get(ginfo[2]) : null;
                setCellText(row.getCell(1 + dateColumns.size() + g),
                        formatGrowthRate(curVal, prevVal), false, null);
            }
        }

        // 资产负债率行
        int lastRow = 1 + BALANCE_SHEET_KEY_ITEMS.size();
        XWPFTableRow ratioRow = table.getRow(lastRow);
        setCellText(ratioRow.getCell(0), "资产负债率", true, null);
        for (int c = 0; c < dateColumns.size(); c++) {
            String col = dateColumns.get(c);
            Map<String, BigDecimal> assetVals = itemDateValues.get(ITEM_ASSET_TOTAL);
            Map<String, BigDecimal> liabilityVals = itemDateValues.get(ITEM_LIABILITY_TOTAL);
            BigDecimal asset = assetVals != null ? assetVals.get(col) : null;
            BigDecimal liability = liabilityVals != null ? liabilityVals.get(col) : null;
            setCellText(ratioRow.getCell(1 + c), formatLiabilityRatio(asset, liability), false, null);
        }
        for (int g = 0; g < growthCols.size(); g++) {
            String[] ginfo = growthCols.get(g);
            Map<String, BigDecimal> assetVals = itemDateValues.get(ITEM_ASSET_TOTAL);
            Map<String, BigDecimal> liabilityVals = itemDateValues.get(ITEM_LIABILITY_TOTAL);
            BigDecimal curAsset = assetVals != null ? assetVals.get(ginfo[1]) : null;
            BigDecimal curLiability = liabilityVals != null ? liabilityVals.get(ginfo[1]) : null;
            BigDecimal prevAsset = assetVals != null ? assetVals.get(ginfo[2]) : null;
            BigDecimal prevLiability = liabilityVals != null ? liabilityVals.get(ginfo[2]) : null;
            BigDecimal curRatio = calcLiabilityRatio(curAsset, curLiability);
            BigDecimal prevRatio = calcLiabilityRatio(prevAsset, prevLiability);
            setCellText(ratioRow.getCell(1 + dateColumns.size() + g),
                    formatGrowthRate(curRatio, prevRatio), false, null);
        }
    }

    /**
     * 向已创建的空白表格填充利润表关键科目数据。
     */
    private void fillProfitSheetTable(XWPFTable table,
                                       Map<String, Map<String, BigDecimal>> itemDateValues,
                                       List<String> dateColumns) {
        List<String[]> growthCols = buildGrowthCols(dateColumns);
        int colCount = 1 + dateColumns.size() + growthCols.size();
        int rowCount = 1 + PROFIT_SHEET_KEY_ITEMS.size() + 3;

        for (int r = 0; r < rowCount; r++) {
            ensureCellCount(table.createRow(), colCount);
        }

        // 表头行
        XWPFTableRow headerRow = table.getRow(0);
        setCellText(headerRow.getCell(0), "项目", true, null);
        for (int i = 0; i < dateColumns.size(); i++) {
            setCellText(headerRow.getCell(1 + i), dateColumns.get(i), true, null);
        }
        for (int i = 0; i < growthCols.size(); i++) {
            setCellText(headerRow.getCell(1 + dateColumns.size() + i), growthCols.get(i)[0], true, null);
        }

        // 9 个基本科目
        for (int r = 0; r < PROFIT_SHEET_KEY_ITEMS.size(); r++) {
            String item = PROFIT_SHEET_KEY_ITEMS.get(r);
            XWPFTableRow row = table.getRow(1 + r);
            setCellText(row.getCell(0), item, false, null);

            Map<String, BigDecimal> values = itemDateValues.get(item);
            for (int c = 0; c < dateColumns.size(); c++) {
                BigDecimal val = values != null ? values.get(dateColumns.get(c)) : null;
                setCellText(row.getCell(1 + c), formatAmount(val), false, null);
            }
            for (int g = 0; g < growthCols.size(); g++) {
                String[] ginfo = growthCols.get(g);
                BigDecimal curVal = values != null ? values.get(ginfo[1]) : null;
                BigDecimal prevVal = values != null ? values.get(ginfo[2]) : null;
                setCellText(row.getCell(1 + dateColumns.size() + g),
                        formatGrowthRate(curVal, prevVal), false, null);
            }
        }

        // 计算行
        int base = 1 + PROFIT_SHEET_KEY_ITEMS.size();
        fillRatioRowAt(table, base, itemDateValues, dateColumns, growthCols, "毛利润率");
        fillRatioRowAt(table, base + 1, itemDateValues, dateColumns, growthCols, "净利润率");
        fillRatioRowAt(table, base + 2, itemDateValues, dateColumns, growthCols, "研发费用占比");
    }

    /**
     * 填充利润表计算行（毛利润率/净利润率/研发费用占比）的值和增长率。
     */
    private void fillRatioRowAt(XWPFTable table, int rowIdx,
                                 Map<String, Map<String, BigDecimal>> itemDateValues, List<String> dateColumns,
                                 List<String[]> growthCols, String label) {

        XWPFTableRow row = table.getRow(rowIdx);
        setCellText(row.getCell(0), label, false, null);

        for (int c = 0; c < dateColumns.size(); c++) {
            String col = dateColumns.get(c);
            String val;
            if ("毛利润率".equals(label)) {
                Map<String, BigDecimal> revVals = itemDateValues.get(ITEM_REVENUE);
                Map<String, BigDecimal> costVals = itemDateValues.get(ITEM_COST);
                BigDecimal rev = revVals != null ? revVals.get(col) : null;
                BigDecimal cost = costVals != null ? costVals.get(col) : null;
                val = (rev != null && cost != null && rev.compareTo(BigDecimal.ZERO) != 0)
                        ? formatRatio(rev.subtract(cost), rev) : "-";
            } else if ("净利润率".equals(label)) {
                Map<String, BigDecimal> revVals = itemDateValues.get(ITEM_REVENUE);
                Map<String, BigDecimal> profitVals = itemDateValues.get(ITEM_NET_PROFIT);
                val = formatRatio(
                        profitVals != null ? profitVals.get(col) : null,
                        revVals != null ? revVals.get(col) : null);
            } else {
                Map<String, BigDecimal> revVals = itemDateValues.get(ITEM_REVENUE);
                Map<String, BigDecimal> rdVals = itemDateValues.get(ITEM_RD_EXPENSE);
                val = formatRatio(
                        rdVals != null ? rdVals.get(col) : null,
                        revVals != null ? revVals.get(col) : null);
            }
            setCellText(row.getCell(1 + c), val, false, null);
        }

        // 增长率
        for (int g = 0; g < growthCols.size(); g++) {
            String[] ginfo = growthCols.get(g);
            BigDecimal curRatio = computeRatioForLabel(itemDateValues, ginfo[1], label);
            BigDecimal prevRatio = computeRatioForLabel(itemDateValues, ginfo[2], label);
            setCellText(row.getCell(1 + dateColumns.size() + g),
                    formatGrowthRate(curRatio, prevRatio), false, null);
        }
    }

    /**
     * 计算指定标签（毛利润率/净利润率/研发费用占比）的比率值。
     */
    private BigDecimal computeRatioForLabel(Map<String, Map<String, BigDecimal>> itemDateValues,
                                             String col, String label) {
        if ("毛利润率".equals(label)) {
            Map<String, BigDecimal> revVals = itemDateValues.get(ITEM_REVENUE);
            Map<String, BigDecimal> costVals = itemDateValues.get(ITEM_COST);
            BigDecimal rev = revVals != null ? revVals.get(col) : null;
            BigDecimal cost = costVals != null ? costVals.get(col) : null;
            if (rev != null && cost != null && rev.compareTo(BigDecimal.ZERO) != 0) {
                return rev.subtract(cost).divide(rev, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }
        } else if ("净利润率".equals(label)) {
            Map<String, BigDecimal> revVals = itemDateValues.get(ITEM_REVENUE);
            Map<String, BigDecimal> profitVals = itemDateValues.get(ITEM_NET_PROFIT);
            BigDecimal rev = revVals != null ? revVals.get(col) : null;
            BigDecimal profit = profitVals != null ? profitVals.get(col) : null;
            if (profit != null && rev != null && rev.compareTo(BigDecimal.ZERO) != 0) {
                return profit.divide(rev, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }
        } else {
            Map<String, BigDecimal> revVals = itemDateValues.get(ITEM_REVENUE);
            Map<String, BigDecimal> rdVals = itemDateValues.get(ITEM_RD_EXPENSE);
            BigDecimal rev = revVals != null ? revVals.get(col) : null;
            BigDecimal rd = rdVals != null ? rdVals.get(col) : null;
            if (rd != null && rev != null && rev.compareTo(BigDecimal.ZERO) != 0) {
                return rd.divide(rev, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }
        }
        return null;
    }

    /**
     * 确保表格行有足够的单元格。
     */
    private void ensureCellCount(XWPFTableRow row, int count) {
        while (row.getTableCells().size() < count) {
            row.addNewTableCell();
        }
    }

    /**
     * 设置表格单元格文本和样式。
     */
    private void setCellText(XWPFTableCell cell, String text, boolean bold, String colorHex) {
        XWPFParagraph p = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
        p.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun r = p.createRun();
        r.setText(text != null ? text : "");
        r.setFontSize(10);
        r.setFontFamily("微软雅黑");
        r.setBold(bold);
        if (colorHex != null) {
            r.setColor(colorHex);
        }
    }

    /**
     * 构建增长率列信息列表。
     * 每个元素为 String[3]: [header, curCol, prevCol]
     */
    private List<String[]> buildGrowthCols(List<String> dateColumns) {
        List<String> yearEndCols = dateColumns.stream()
                .filter(col -> col.matches("\\d{4}年"))
                .collect(Collectors.toList());
        List<String[]> result = new ArrayList<>();
        for (int i = 0; i < yearEndCols.size() - 1; i++) {
            result.add(new String[]{
                    extractYearFromColumn(yearEndCols.get(i)) + "年增长率",
                    yearEndCols.get(i),
                    yearEndCols.get(i + 1)
            });
        }
        return result;
    }
    @lombok.AllArgsConstructor
    private static class SubmitFileMeta {
        final String attId;
        String businessType;          // 初始为 "finance"/"business"，build 时回填为 docTypeId 值
        final String reportDate;
    }
}
