-- ============================================================
-- techfin-platform 数据库表初始化脚本
-- 数据库：MySQL 8.0+ (InnoDB, utf8mb4)
-- 说明：MyBatis-Plus 管理数据访问，需手动建表
-- ============================================================

CREATE DATABASE IF NOT EXISTS mydb
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE mydb;

-- ------------------------------------------------------------
-- 1. application_att — 附件元信息表（独立实体）
--    上传时写入，提交时查询 fileName/fileSize/businessType 用于外部 API
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS application_att (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    att_id        VARCHAR(64)  NOT NULL                COMMENT '附件上传返回的附件 ID',
    file_name     VARCHAR(255) DEFAULT NULL             COMMENT '上传时的原始文件名',
    file_size     BIGINT       DEFAULT NULL             COMMENT '文件大小（字节）',
    created_at    DATETIME     DEFAULT NULL             COMMENT '创建时间，用于清理孤立附件',
    PRIMARY KEY (id),
    UNIQUE KEY uk_att_id (att_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='附件元信息表';


-- ------------------------------------------------------------
-- 2. application_record — 申请记录表
--    提交资料时创建，主键为 task_id，无外键约束
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS application_record (
    task_id     VARCHAR(64)  NOT NULL                COMMENT '任务 ID，格式 TASK-<32位hex>',
    credit_code VARCHAR(18)  NOT NULL                COMMENT '统一社会信用代码',
    customer_no VARCHAR(64)  NOT NULL                COMMENT '客户编号',
    status      VARCHAR(32)  NOT NULL DEFAULT 'UNFINISHED' COMMENT '任务状态：UNFINISHED（未完成）/ COMPLETED（已完成）',
    act_cntlr_nm VARCHAR(200) DEFAULT NULL             COMMENT '实际控制人姓名，用户确认后回填',
    created_at  DATETIME     NOT NULL                COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL                COMMENT '更新时间',
    PRIMARY KEY (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申请记录表';


-- ------------------------------------------------------------
-- 3. application_doc — 文档明细表（集合表）
--    每条记录对应外部 API 返回的一条文档，DOC_ID 全局唯一
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS application_doc (
    doc_id        VARCHAR(64)  NOT NULL                COMMENT '资料批量新增返回的文档 ID',
    task_id       VARCHAR(64)  NOT NULL                COMMENT '关联 application_record.task_id',
    business_type VARCHAR(32)  DEFAULT NULL             COMMENT '业务类型（docTypeId 值），从 application_att 回填',
    report_date   VARCHAR(10)  DEFAULT NULL             COMMENT '财报报告日期（仅 finance 类型有值）',
    PRIMARY KEY (doc_id),
    KEY idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档明细表';
