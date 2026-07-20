-- ======================================================================
-- 文件名：客户信息表_随机数据.sql
-- 目标表：t101_sz_hjy_sxd_profile（善新贷材料生成结果表）
-- 功能：基于表结构及字段备注，使用 MySQL 内置随机函数批量生成 200 条随机测试数据
-- 说明：
--   1. 该表无主键，故直接 INSERT，无需处理主键冲突
--   2. 复用随机函数统一字段生成逻辑，便于维护
--   3. 所有字符串字段长度均控制在列定义上限以内
--   4. 字段值分布参考表定义中的「备注」说明（如 0/1 枚举、缺省值 -99 / - 等）
-- 使用：
--   先执行 docs/客户信息表.sql 建表，再执行本文件
--   CALL generate_random_profile(200);  -- 可改条数
-- ======================================================================

-- 清空旧数据（按需保留或注释掉）
TRUNCATE TABLE t101_sz_hjy_sxd_profile;

-- ---------------------------------------------------------------------
-- 自定义随机函数：集中各字段的随机生成逻辑
-- ---------------------------------------------------------------------
DROP FUNCTION IF EXISTS `rand_cst_id`;
DROP FUNCTION IF EXISTS `rand_credit_code`;
DROP FUNCTION IF EXISTS `rand_cn_name`;
DROP FUNCTION IF EXISTS `rand_company_name`;
DROP FUNCTION IF EXISTS `rand_amount`;
DROP FUNCTION IF EXISTS `rand_date_str`;
DROP FUNCTION IF EXISTS `rand_date_str_dmy`;
DROP FUNCTION IF EXISTS `rand_address`;
DROP FUNCTION IF EXISTS `rand_scope`;
DROP FUNCTION IF EXISTS `rand_default_or_value`;

DELIMITER $$

-- 客户编号：CST + 10位数字
CREATE FUNCTION `rand_cst_id`() RETURNS VARCHAR(200)
DETERMINISTIC
BEGIN
    RETURN CONCAT('CST', LPAD(FLOOR(RAND() * 10000000000), 10, '0'));
END$$

-- 统一社会信用代码：18位（简化为大写字母+数字组合）
CREATE FUNCTION `rand_credit_code`() RETURNS VARCHAR(200)
DETERMINISTIC
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE s VARCHAR(200) DEFAULT '';
    DECLARE ch CHAR(1);
    WHILE i < 18 DO
        SET ch = SUBSTRING('0123456789ABCDEFGHJKLMNPQRTUWXY', FLOOR(RAND() * 31) + 1, 1);
        SET s = CONCAT(s, ch);
        SET i = i + 1;
    END WHILE;
    RETURN s;
END$$

-- 随机中文姓名（姓氏 + 双字名）
CREATE FUNCTION `rand_cn_name`() RETURNS VARCHAR(200)
DETERMINISTIC
BEGIN
    DECLARE surnames VARCHAR(100) DEFAULT '王李张刘陈杨黄赵周吴徐孙马朱胡郭何高林郑谢罗梁宋唐许韩冯邓曹彭曾肖田董袁潘于蒋蔡余杜叶程苏魏吕丁任沈姚卢姜崔钟谭陆汪范金石廖贾夏韦付方白邹孟熊秦邱江薛庞';
    DECLARE given_chars VARCHAR(200) DEFAULT '伟芳娜秀英敏静丽强磊军洋勇艳杰娟涛明超秀霞平刚桂英华建国玉萍红香玉琴文金鑫鹏飞俊豪嘉宁子涵梓轩晨曦浩然若曦梦琪';
    DECLARE sname CHAR(1);
    DECLARE gname VARCHAR(2);
    SET sname = SUBSTRING(surnames, FLOOR(RAND() * CHAR_LENGTH(surnames)) + 1, 1);
    SET gname = SUBSTRING(given_chars, FLOOR(RAND() * CHAR_LENGTH(given_chars)) + 1, 1);
    IF RAND() > 0.5 THEN
        SET gname = CONCAT(gname, SUBSTRING(given_chars, FLOOR(RAND() * CHAR_LENGTH(given_chars)) + 1, 1));
    END IF;
    RETURN CONCAT(sname, gname);
END$$

-- 随机公司名称：地区 + 行业词 + 公司类型
CREATE FUNCTION `rand_company_name`() RETURNS VARCHAR(500)
DETERMINISTIC
BEGIN
    DECLARE regions VARCHAR(200) DEFAULT '深圳市,广州市,北京市,上海市,杭州市,成都市,苏州市,南京市,武汉市,西安市,重庆市,长沙市,青岛市';
    DECLARE industries VARCHAR(300) DEFAULT '科技,信息技术,生物医药,新能源,新材料,智能装备,电子,医药,环保,金融,网络,数据,通信,软件';
    DECLARE suffixes VARCHAR(200) DEFAULT '有限公司,股份有限公司,科技有限公司,集团有限公司,控股有限公司';
    DECLARE region_idx INT;
    DECLARE region VARCHAR(50);
    DECLARE industry VARCHAR(50);
    DECLARE suffix VARCHAR(50);
    SET region_idx := FLOOR(RAND() * 13) + 1;
    SET region := SUBSTRING_INDEX(SUBSTRING_INDEX(regions, ',', region_idx), ',', -1);
    SET industry := SUBSTRING(industries, FLOOR(RAND() * CHAR_LENGTH(industries)) + 1, 4);
    SET suffix := SUBSTRING_INDEX(SUBSTRING_INDEX(suffixes, ',', FLOOR(RAND() * 5) + 1), ',', -1);
    RETURN CONCAT(region, rand_cn_name(), industry, suffix);
END$$

-- 随机正数金额字符串（万元为单位，保留两位小数）
CREATE FUNCTION `rand_amount`(min_wan DECIMAL(12,2), max_wan DECIMAL(12,2)) RETURNS VARCHAR(100)
DETERMINISTIC
BEGIN
    RETURN CAST(ROUND(RAND() * (max_wan - min_wan) + min_wan, 2) AS CHAR);
END$$

-- 随机日期字符串（YYYY-MM-DD），范围 [start_year, end_year]
CREATE FUNCTION `rand_date_str`(start_year INT, end_year INT) RETURNS VARCHAR(50)
DETERMINISTIC
BEGIN
    DECLARE base_date DATE;
    SET base_date := DATE_ADD(MAKEDATE(start_year, 1), INTERVAL FLOOR(RAND() * DATEDIFF(MAKEDATE(end_year, 365), MAKEDATE(start_year, 1))) DAY);
    RETURN DATE_FORMAT(base_date, '%Y-%m-%d');
END$$

-- 随机日期字符串（ddMMMyyyy 格式，如 24Apr2026），范围 [start_year, end_year]
CREATE FUNCTION `rand_date_str_dmy`(start_year INT, end_year INT) RETURNS VARCHAR(50)
DETERMINISTIC
BEGIN
    DECLARE base_date DATE;
    SET base_date := DATE_ADD(MAKEDATE(start_year, 1), INTERVAL FLOOR(RAND() * DATEDIFF(MAKEDATE(end_year, 365), MAKEDATE(start_year, 1))) DAY);
    RETURN DATE_FORMAT(base_date, '%d%b%Y');
END$$

-- 随机注册地址
CREATE FUNCTION `rand_address`() RETURNS TEXT
DETERMINISTIC
BEGIN
    DECLARE cities VARCHAR(200) DEFAULT '深圳市,广州市,北京市,上海市,杭州市,成都市,苏州市,南京市,武汉市,西安市';
    DECLARE districts VARCHAR(300) DEFAULT '南山区,福田区,罗湖区,宝安区,龙华区,龙岗区,天河区,越秀区,海淀区,朝阳区,浦东新区,西湖区,武侯区,高新区';
    DECLARE roads VARCHAR(300) DEFAULT '科技园路,科苑路,深南大道,滨海大道,科技中一路,高新南一道,软件产业基地,创业路,后海大道,前海路';
    DECLARE city VARCHAR(50);
    DECLARE district VARCHAR(50);
    DECLARE road VARCHAR(50);
    SET city := SUBSTRING_INDEX(SUBSTRING_INDEX(cities, ',', FLOOR(RAND()*10)+1), ',', -1);
    SET district := SUBSTRING_INDEX(SUBSTRING_INDEX(districts, ',', FLOOR(RAND()*14)+1), ',', -1);
    SET road := SUBSTRING_INDEX(SUBSTRING_INDEX(roads, ',', FLOOR(RAND()*10)+1), ',', -1);
    RETURN CONCAT(city, district, road, FLOOR(RAND()*200)+1, '号', FLOOR(RAND()*30)+1, '楼');
END$$

-- 随机经营范围
CREATE FUNCTION `rand_scope`() RETURNS TEXT
DETERMINISTIC
BEGIN
    DECLARE items VARCHAR(500) DEFAULT '技术开发,技术咨询,技术服务,技术转让,软件开发,信息系统集成,数据处理,集成电路设计,生物制品研发,药品生产,医疗器械销售,电子产品制造,进出口贸易,投资管理,经济信息咨询';
    DECLARE n INT DEFAULT FLOOR(RAND()*3)+2;  -- 随机选 2~4 项
    DECLARE result TEXT DEFAULT '';
    DECLARE i INT DEFAULT 0;
    DECLARE idx INT;
    WHILE i < n DO
        SET idx := FLOOR(RAND()*15)+1;
        SET result := CONCAT(result, IF(i>0, '、', ''), SUBSTRING_INDEX(SUBSTRING_INDEX(items, ',', idx), ',', -1));
        SET i := i + 1;
    END WHILE;
    RETURN CONCAT(result, '。（依法须经批准的项目，经相关部门批准后方可开展经营活动）');
END$$

-- 按概率返回一个值或缺省标记：p_default 为返回缺省值的概率(0~1)
-- 返回 default_val 或一个由生成函数 gen_func 计算的值
-- 由于 MySQL 函数不能动态执行 SQL，这里直接在外层用 IF 逻辑，此函数仅作说明，不再定义

DELIMITER ;

-- ---------------------------------------------------------------------
-- 生成随机数据的存储过程
-- 参数 p_count：生成记录条数
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `generate_random_profile`;
DELIMITER $$
CREATE PROCEDURE `generate_random_profile`(IN p_count INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE v_if_loan VARCHAR(50);
    DECLARE v_yuqi_flag VARCHAR(50);
    DECLARE v_kc_score VARCHAR(50);
    DECLARE v_entp_ptnt VARCHAR(50);
    DECLARE v_entp_ptnt_new VARCHAR(50);
    DECLARE v_entp_ivt VARCHAR(50);
    DECLARE v_clst5 VARCHAR(50);

    WHILE i < p_count DO
        -- 是否我行贷款客户（0：否，1：是），贷款客户占 65%
        SET v_if_loan := IF(RAND() < 0.65, '1', '0');
        -- 近两年逾期：仅贷款客户可能逾期，整体逾期率约 10%
        SET v_yuqi_flag := IF(v_if_loan = '1' AND RAND() < 0.10, '1', '0');

        -- 科创分：5% 概率为 -99（没匹配到数据），其余 0~500 之间带两位小数
        IF RAND() < 0.05 THEN
            SET v_kc_score := '-99';
        ELSE
            SET v_kc_score := CONCAT(CAST(FLOOR(RAND() * 500) AS CHAR), '.', LPAD(CAST(FLOOR(RAND() * 100) AS CHAR), 2, '0'));
        END IF;

        -- 专利数量/实用新型/发明/软著：10% 概率为 -99，其余匹配备注示例的小数值
        IF RAND() < 0.10 THEN SET v_entp_ptnt := '-99'; ELSE SET v_entp_ptnt := CAST(FLOOR(RAND() * 50) + 1 AS CHAR); END IF;
        IF RAND() < 0.10 THEN SET v_entp_ptnt_new := '-99'; ELSE SET v_entp_ptnt_new := CAST(FLOOR(RAND() * 30) + 1 AS CHAR); END IF;
        IF RAND() < 0.15 THEN SET v_entp_ivt := '-99'; ELSE SET v_entp_ivt := CAST(FLOOR(RAND() * 20) AS CHAR); END IF;
        IF RAND() < 0.15 THEN SET v_clst5 := '-99'; ELSE SET v_clst5 := CAST(FLOOR(RAND() * 30) AS CHAR); END IF;

        INSERT INTO t101_sz_hjy_sxd_profile (
            data_bsn_dt, etl_dt, cst_id, cst_nm, fd_dt, lgl_rprs_nm, act_cntlr_nm,
            rgst_cpamt, arcptl_cpamt, credit_code, CPCT_TPCD, entp_sz_cd,
            dtl_adr, org_oprt_scop_dsc, entp_bliy, tech_tag, tech_flow, kc_score,
            ENTP_PTNT_NUM, ENTPPRCTNEWTPPTNT_NUM, ENTP_IVT_PTNT_NUM, CLST5YRINNRSWCOPR_NUM,
            if_loan, product_name, loan_amount, loan_term, loan_balance,
            dep_bal, dep_bal_dt, dep_aadbal, acc_start_dt, acc_type,
            isug_pnum, avg_12_isug_amt, if_yuqi, ltgtrltd_ind, if_rad_alarm,
            cst_mngacc_cstmgr_id, cst_mngacc_inst_supr_insid,
            byzd1, byzd2, byzd3, byzd4, byzd5, byzd6, byzd7, byzd8, byzd9, byzd10
        ) VALUES (
            -- data_bsn_dt / etl_dt：近 1 年内的业务日期和跑批日期
            STR_TO_DATE(rand_date_str(2025, 2026), '%Y-%m-%d'),
            STR_TO_DATE(rand_date_str(2025, 2026), '%Y-%m-%d'),
            rand_cst_id(),
            rand_company_name(),
            rand_date_str(1990, 2024),                                          -- 成立时间（YYYY-MM-DD）
            rand_cn_name(),                                                     -- 法定代表人
            rand_cn_name(),                                                    -- 实控人姓名
            CONCAT(rand_amount(100, 50000), '万元'),                            -- 注册资本
            CONCAT(rand_amount(50, 30000), '万元'),                            -- 实收资本
            rand_credit_code(),
            ELT(FLOOR(RAND()*4)+1, '有限责任公司','股份有限公司','私营独资企业','外商投资企业'),  -- 企业类型
            -- 企业规模：小型占 60%，中型 25%，微型 15%（备注示例为"小型"）
            ELT(FLOOR(RAND()*20)+1,
                '小型','小型','小型','小型','小型','小型','小型','小型','小型','小型',
                '小型','小型','中型','中型','中型','中型','中型','微型','微型','微型'),
            rand_address(),
            rand_scope(),
            -- 所属行业：使用备注示例的层级格式（批发和零售业-零售业-综合零售-其他综合零售）
            ELT(FLOOR(RAND()*8)+1,
                '批发和零售业-零售业-综合零售-其他综合零售',
                '制造业-计算机、通信和其他电子设备制造业-电子器件制造-半导体分立器件制造',
                '信息传输、软件和信息技术服务业-软件和信息技术服务业-软件开发-应用软件开发',
                '制造业-医药制造业-化学药品制剂制造-化学药品制剂制造',
                '科学研究和技术服务业-研究和试验发展-工程和技术研究和试验发展',
                '制造业-专用设备制造业-环保专用设备制造-大气污染防治设备制造',
                '批发和零售业-批发业-机械设备批发-计算机及通讯设备批发',
                '制造业-电气机械和器材制造业-电池制造-锂离子电池制造'),
            -- 企业科技资质类型：参考备注"科技型中小企业 专精特新小巨人 创新型中小企业"
            ELT(FLOOR(RAND()*8)+1,
                '科技型中小企业',
                '专精特新小巨人',
                '创新型中小企业',
                '国家高新技术企业',
                '科技型中小企业 专精特新小巨人',
                '国家高新技术企业 科技型中小企业',
                '国家高新技术企业 创新型中小企业 专精特新小巨人',
                ''),
            -- 我行"技术流"评价结果：参考备注 T7 / -
            IF(RAND() < 0.10, '-',
                CONCAT('T', CAST(FLOOR(RAND() * 10) + 1 AS CHAR))),
            v_kc_score,
            v_entp_ptnt,           -- 企业专利数量
            v_entp_ptnt_new,       -- 企业实用新型专利数
            v_entp_ivt,            -- 企业发明专利数
            v_clst5,               -- 企业软件著作权数量
            v_if_loan,
            -- 最新贷款品种：备注重为"流动资金贷款"
            IF(v_if_loan = '1',
                ELT(FLOOR(RAND()*5)+1, '流动资金贷款','固定资产贷款','善新贷','科创贷','小微企业贷款'),
                NULL),
            -- 贷款金额（元）：参考备注 2200000.00
            IF(v_if_loan = '1', rand_amount(500000, 10000000), NULL),
            -- 贷款期限（月）：参考备注 36（纯数字，不带单位）
            IF(v_if_loan = '1', CAST(FLOOR(RAND()*60) + 6 AS CHAR), NULL),
            -- 贷款当前余额：结清后为 0
            IF(v_if_loan = '1',
                IF(RAND() < 0.15, '0', rand_amount(0, 8000000)),
                NULL),
            -- 对公存款余额：参考备注 6359.4
            rand_amount(100, 500000),
            -- 对公存款余额日期：ddMMMyyyy 格式，参考备注 24Apr2026
            rand_date_str_dmy(2025, 2026),
            -- 对公存款年日均余额：参考备注 318.13
            rand_amount(50, 300000),
            -- 开立账户时间：ddMMMyyyy 格式，参考备注 24Apr2024
            rand_date_str_dmy(2010, 2024),
            -- 对公账户类型：参考备注"基本户"
            ELT(FLOOR(RAND()*2)+1, '基本户','一般户'),
            -- 代发人数：参考备注 2
            CAST(FLOOR(RAND()*50) + 1 AS CHAR),
            -- 月均代发工资（元）：参考备注 15510
            CAST(ROUND(RAND()*80000 + 3000, 2) AS CHAR),
            v_yuqi_flag,                                                    -- 近两年逾期（0：否，1：是）
            ELT(FLOOR(RAND()*2)+1, '0','1'),                                -- 司法纠纷（0：否，1：是）
            ELT(FLOOR(RAND()*2)+1, '0','1'),                                -- RAD红色预警（0：否，1：是）
            CONCAT('MGR', LPAD(FLOOR(RAND()*99999), 5, '0')),               -- 管户客户经理编号
            CONCAT('BANK', LPAD(FLOOR(RAND()*999999), 6, '0')),             -- 管户支行编号
            NULL, NULL, NULL, NULL, NULL,                                   -- 备用字段 1-5
            NULL, NULL, NULL, NULL, NULL                                    -- 备用字段 6-10
        );

        SET i := i + 1;
    END WHILE;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------
-- 执行：生成 200 条
-- ---------------------------------------------------------------------
CALL generate_random_profile(200);

-- 可选：清理自定义函数（如需保留可注释）
-- DROP FUNCTION IF EXISTS `rand_cst_id`;
-- DROP FUNCTION IF EXISTS `rand_credit_code`;
-- DROP FUNCTION IF EXISTS `rand_cn_name`;
-- DROP FUNCTION IF EXISTS `rand_company_name`;
-- DROP FUNCTION IF EXISTS `rand_amount`;
-- DROP FUNCTION IF EXISTS `rand_date_str`;
-- DROP FUNCTION IF EXISTS `rand_date_str_dmy`;
-- DROP FUNCTION IF EXISTS `rand_address`;
-- DROP FUNCTION IF EXISTS `rand_scope`;
-- DROP PROCEDURE IF EXISTS `generate_random_profile`;

-- 验证
SELECT COUNT(*) AS total_rows FROM t101_sz_hjy_sxd_profile;
SELECT * FROM t101_sz_hjy_sxd_profile LIMIT 5;
