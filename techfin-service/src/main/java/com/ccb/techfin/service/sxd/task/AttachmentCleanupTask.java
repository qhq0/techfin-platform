package com.ccb.techfin.service.sxd.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccb.techfin.dao.sxd.AttachmentMapper;
import com.ccb.techfin.dao.sxd.DocEntryMapper;
import com.ccb.techfin.dao.sxd.SxdMapper;
import com.ccb.techfin.model.sxd.entity.ApplicationAttachment;
import com.ccb.techfin.model.sxd.entity.ApplicationRecord;
import com.ccb.techfin.model.sxd.entity.DocEntry;
import com.ccb.techfin.model.sxd.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 定时清理任务。
 * <ul>
 *   <li>每天凌晨 2:00 清理 application_att 中创建超过 24 小时的孤立记录</li>
 *   <li>每天凌晨 2:00 清理 application_doc 中关联状态为 UNFINISHED 的记录</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttachmentCleanupTask {

    private final AttachmentMapper attachmentMapper;
    private final DocEntryMapper docEntryMapper;
    private final SxdMapper sxdMapper;

    /**
     * 每天凌晨 2:00 执行清理。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanup() {
        log.info("Scheduled cleanup task started");
        cleanupOrphanAttachments();
        cleanupUnfinishedDocEntries();
        log.info("Scheduled cleanup task completed");
    }

    /**
     * 清理 application_att 中创建时间超过 24 小时且未被引用的孤立附件记录。
     */
    private void cleanupOrphanAttachments() {
        LocalDateTime deadline = LocalDateTime.now().minusHours(24);
        List<ApplicationAttachment> oldRecords = attachmentMapper.selectList(
                new LambdaQueryWrapper<ApplicationAttachment>()
                        .isNotNull(ApplicationAttachment::getCreatedAt)
                        .lt(ApplicationAttachment::getCreatedAt, deadline));

        if (oldRecords.isEmpty()) {
            log.debug("No orphan attachments to clean up");
            return;
        }

        List<Long> ids = oldRecords.stream()
                .map(ApplicationAttachment::getId)
                .collect(Collectors.toList());
        int deleted = attachmentMapper.delete(
                new LambdaQueryWrapper<ApplicationAttachment>()
                        .in(ApplicationAttachment::getId, ids));
        log.info("Cleaned up {} orphan attachment(s) older than 24h", deleted);
    }

    /**
     * 清理 application_doc 中关联任务状态为 UNFINISHED 的记录。
     * 先查找所有 UNFINISHED 的 task_id，再批量删除对应的文档记录。
     */
    private void cleanupUnfinishedDocEntries() {
        List<ApplicationRecord> unfinishedRecords = sxdMapper.selectList(
                new LambdaQueryWrapper<ApplicationRecord>()
                        .eq(ApplicationRecord::getStatus, TaskStatus.UNFINISHED));

        if (unfinishedRecords.isEmpty()) {
            log.debug("No unfinished tasks to clean up doc entries");
            return;
        }

        List<String> taskIds = unfinishedRecords.stream()
                .map(ApplicationRecord::getTaskId)
                .collect(Collectors.toList());

        int deleted = 0;
        for (String taskId : taskIds) {
            int count = docEntryMapper.delete(
                    new LambdaQueryWrapper<DocEntry>()
                            .eq(DocEntry::getTaskId, taskId));
            deleted += count;
            if (count > 0) {
                log.info("Cleaned up {} doc entry(ies) for unfinished taskId={}", count, taskId);
            }
        }
        log.info("Cleaned up total {} doc entry(ies) for unfinished tasks", deleted);
    }
}
