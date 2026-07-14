package com.ccb.techfin.model.sxd.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class
FileUploadResult {

    /** 本次上传成功的附件 ID */
    private String attId;
}
