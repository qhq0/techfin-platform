package com.ccb.techfin.model.sxd.entity;

import jakarta.persistence.*;
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
@Entity
@Table(name = "application_att")
public class ApplicationAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 附件上传返回的附件 ID */
    @Column(name = "att_id", nullable = false, length = 64)
    private String attId;

    /** 上传时的原始文件名 */
    @Column(name = "file_name", length = 255)
    private String fileName;

    /** 文件大小（字节） */
    @Column(name = "file_size")
    private Long fileSize;

    /** 业务类型（finance / business） */
    @Column(name = "business_type", length = 32)
    private String businessType;
}
