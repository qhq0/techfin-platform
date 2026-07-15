package com.ccb.techfin.model.sxd.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 客户信息表（善新贷材料生成结果表）
 * 映射 t101_sz_hjy_sxd_profile
 */
@Data
@TableName("t101_sz_hjy_sxd_profile")
public class CustomerProfile {

    /** 客户编号（主键） */
    @TableId("cst_id")
    private String cstId;

    /** 客户名称 */
    @TableField("cst_nm")
    private String cstNm;

    /** 实控人姓名 */
    @TableField("act_cntlr_nm")
    private String actCntlrNm;

    /** 数据业务日期（用于排序取最新） */
    @TableField("data_bsn_dt")
    private LocalDate dataBsnDt;
}
