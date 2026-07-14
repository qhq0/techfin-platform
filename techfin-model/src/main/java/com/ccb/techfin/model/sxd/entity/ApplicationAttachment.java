package com.ccb.techfin.model.sxd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 附件上传记录，独立实体映射 application_att 表。
 * 上传时记录文件元信息，提交时根据 attId 查找匹配。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("application_att")
public class ApplicationAttachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 附件上传返回的附件 ID */
    @TableField("att_id")
    private String attId;

    /** 上传时的原始文件名 */
    @TableField("file_name")
    private String fileName;

    /** 文件大小（字节） */
    @TableField("file_size")
    private Long fileSize;

    /** 业务类型（finance / business） */
    @TableField("business_type")
    private String businessType;
}
