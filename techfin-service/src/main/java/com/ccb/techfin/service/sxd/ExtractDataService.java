package com.ccb.techfin.service.sxd;

import com.ccb.techfin.model.sxd.dto.response.ExtractDataResponse;

public interface ExtractDataService {

    /**
     * 查询商业计划书的提取数据。先从缓存表读取，缓存未命中时调用外部 API 并写入缓存。
     *
     * @param taskId 申请记录的任务 ID
     * @return 提取数据（按 tableName 分组）
     */
    ExtractDataResponse queryExtractData(String taskId);

    /**
     * 导出商业计划书的提取结果 xlsx 文件。
     *
     * @param taskId 申请记录的任务 ID
     * @return xlsx 文件字节数组
     */
    byte[] exportBusinessExtractData(String taskId);

    /**
     * 导出财务报表的提取结果 zip 压缩包。
     *
     * @param taskId 申请记录的任务 ID
     * @return zip 文件字节数组
     */
    byte[] exportFinanceExtractData(String taskId);

    /**
     * 生成 Word 报告，包含企业基本信息、商业计划书提取文本、资产负债表关键科目和利润表关键科目。
     *
     * @param taskId 申请记录的任务 ID
     * @param cstId  客户编号
     * @return Word 文档（.docx）字节数组
     */
    byte[] generateReport(String taskId, String cstId);
}