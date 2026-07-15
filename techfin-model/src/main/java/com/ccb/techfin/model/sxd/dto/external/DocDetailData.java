package com.ccb.techfin.model.sxd.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 资料详情接口（doc/get/{doc_id}）返回的 data 结构。
 * extractState 取值：U|I|F|Y — 待执行|执行中|执行失败|执行完成
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocDetailData {

    private String id;
    private String docName;
    private String projectId;
    private String attId;
    private String extractState;
}
