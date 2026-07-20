-- ======================================================================
-- 文件名：sxd_profile_数据生成.sql
-- 目标表：sxd_profile（善新贷材料生成结果表）
-- 功能：从 t101_sz_hjy_sxd_profile 表取数（去掉 etl_dt 字段），
--       按客户编号 cst_id 去重后取最新记录，插入 sxd_profile
-- 说明：
--   1. 源表 t101_sz_hjy_sxd_profile 的无主键，同一 cst_id 可能有多条，
--      本脚本按 data_bsn_dt 降序取最新的一条
--   2. sxd_profile 以 cst_id 为主键，使用 INSERT IGNORE 避免冲突
-- 使用：
--   先执行 docs/sxd_profile.sql 建表，再执行本文件
--   CALL generate_sxd_profile(200);  -- 可改条数，不传则取全部
-- ======================================================================

-- 清空旧数据（按需保留或注释掉）
TRUNCATE TABLE sxd_profile;

-- ---------------------------------------------------------------------
-- 生成数据的存储过程
-- 参数 p_count：最多取多少条记录（NULL 或 0 表示全部）
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `generate_sxd_profile`;
DELIMITER $$
CREATE PROCEDURE `generate_sxd_profile`(IN p_count INT)
BEGIN
    -- 使用 INSERT IGNORE + ROW_NUMBER 按 cst_id 去重取最新记录
    -- p_count <= 0 或 NULL 时取全部，否则限制条数
    IF p_count IS NULL OR p_count <= 0 THEN
        INSERT IGNORE INTO sxd_profile (
            data_bsn_dt, cst_id, cst_nm, fd_dt, lgl_rprs_nm, act_cntlr_nm,
            rgst_cpamt, arcptl_cpamt, credit_code, CPCT_TPCD, entp_sz_cd,
            dtl_adr, org_oprt_scop_dsc, entp_bliy, tech_tag, tech_flow, kc_score,
            ENTP_PTNT_NUM, ENTPPRCTNEWTPPTNT_NUM, ENTP_IVT_PTNT_NUM, CLST5YRINNRSWCOPR_NUM,
            if_loan, product_name, loan_amount, loan_term, loan_balance,
            dep_bal, dep_bal_dt, dep_aadbal, acc_start_dt, acc_type,
            isug_pnum, avg_12_isug_amt, if_yuqi, ltgtrltd_ind, if_rad_alarm,
            cst_mngacc_cstmgr_id, cst_mngacc_inst_supr_insid,
            byzd1, byzd2, byzd3, byzd4, byzd5, byzd6, byzd7, byzd8, byzd9, byzd10
        )
        SELECT
            ranked.data_bsn_dt, ranked.cst_id, ranked.cst_nm, ranked.fd_dt,
            ranked.lgl_rprs_nm, ranked.act_cntlr_nm, ranked.rgst_cpamt,
            ranked.arcptl_cpamt, ranked.credit_code, ranked.CPCT_TPCD,
            ranked.entp_sz_cd, ranked.dtl_adr, ranked.org_oprt_scop_dsc,
            ranked.entp_bliy, ranked.tech_tag, ranked.tech_flow, ranked.kc_score,
            ranked.ENTP_PTNT_NUM, ranked.ENTPPRCTNEWTPPTNT_NUM,
            ranked.ENTP_IVT_PTNT_NUM, ranked.CLST5YRINNRSWCOPR_NUM,
            ranked.if_loan, ranked.product_name, ranked.loan_amount,
            ranked.loan_term, ranked.loan_balance, ranked.dep_bal,
            ranked.dep_bal_dt, ranked.dep_aadbal, ranked.acc_start_dt,
            ranked.acc_type, ranked.isug_pnum, ranked.avg_12_isug_amt,
            ranked.if_yuqi, ranked.ltgtrltd_ind, ranked.if_rad_alarm,
            ranked.cst_mngacc_cstmgr_id, ranked.cst_mngacc_inst_supr_insid,
            ranked.byzd1, ranked.byzd2, ranked.byzd3, ranked.byzd4, ranked.byzd5,
            ranked.byzd6, ranked.byzd7, ranked.byzd8, ranked.byzd9, ranked.byzd10
        FROM (
            SELECT t.*,
                   ROW_NUMBER() OVER (PARTITION BY t.cst_id ORDER BY t.data_bsn_dt DESC) AS rn
            FROM t101_sz_hjy_sxd_profile t
        ) ranked
        WHERE ranked.rn = 1
        ORDER BY ranked.cst_id;
    ELSE
        INSERT IGNORE INTO sxd_profile (
            data_bsn_dt, cst_id, cst_nm, fd_dt, lgl_rprs_nm, act_cntlr_nm,
            rgst_cpamt, arcptl_cpamt, credit_code, CPCT_TPCD, entp_sz_cd,
            dtl_adr, org_oprt_scop_dsc, entp_bliy, tech_tag, tech_flow, kc_score,
            ENTP_PTNT_NUM, ENTPPRCTNEWTPPTNT_NUM, ENTP_IVT_PTNT_NUM, CLST5YRINNRSWCOPR_NUM,
            if_loan, product_name, loan_amount, loan_term, loan_balance,
            dep_bal, dep_bal_dt, dep_aadbal, acc_start_dt, acc_type,
            isug_pnum, avg_12_isug_amt, if_yuqi, ltgtrltd_ind, if_rad_alarm,
            cst_mngacc_cstmgr_id, cst_mngacc_inst_supr_insid,
            byzd1, byzd2, byzd3, byzd4, byzd5, byzd6, byzd7, byzd8, byzd9, byzd10
        )
        SELECT
            ranked.data_bsn_dt, ranked.cst_id, ranked.cst_nm, ranked.fd_dt,
            ranked.lgl_rprs_nm, ranked.act_cntlr_nm, ranked.rgst_cpamt,
            ranked.arcptl_cpamt, ranked.credit_code, ranked.CPCT_TPCD,
            ranked.entp_sz_cd, ranked.dtl_adr, ranked.org_oprt_scop_dsc,
            ranked.entp_bliy, ranked.tech_tag, ranked.tech_flow, ranked.kc_score,
            ranked.ENTP_PTNT_NUM, ranked.ENTPPRCTNEWTPPTNT_NUM,
            ranked.ENTP_IVT_PTNT_NUM, ranked.CLST5YRINNRSWCOPR_NUM,
            ranked.if_loan, ranked.product_name, ranked.loan_amount,
            ranked.loan_term, ranked.loan_balance, ranked.dep_bal,
            ranked.dep_bal_dt, ranked.dep_aadbal, ranked.acc_start_dt,
            ranked.acc_type, ranked.isug_pnum, ranked.avg_12_isug_amt,
            ranked.if_yuqi, ranked.ltgtrltd_ind, ranked.if_rad_alarm,
            ranked.cst_mngacc_cstmgr_id, ranked.cst_mngacc_inst_supr_insid,
            ranked.byzd1, ranked.byzd2, ranked.byzd3, ranked.byzd4, ranked.byzd5,
            ranked.byzd6, ranked.byzd7, ranked.byzd8, ranked.byzd9, ranked.byzd10
        FROM (
            SELECT t.*,
                   ROW_NUMBER() OVER (PARTITION BY t.cst_id ORDER BY t.data_bsn_dt DESC) AS rn
            FROM t101_sz_hjy_sxd_profile t
        ) ranked
        WHERE ranked.rn = 1
        ORDER BY ranked.cst_id
        LIMIT p_count;
    END IF;

    -- 输出实际插入条数
    SELECT ROW_COUNT() AS inserted_rows;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------
-- 执行：取全部数据
-- ---------------------------------------------------------------------
CALL generate_sxd_profile(0);

-- 验证
SELECT COUNT(*) AS total_rows FROM sxd_profile;
SELECT * FROM sxd_profile LIMIT 5;
