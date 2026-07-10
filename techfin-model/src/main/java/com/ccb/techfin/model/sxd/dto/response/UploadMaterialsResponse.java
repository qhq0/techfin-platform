package com.ccb.techfin.model.sxd.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadMaterialsResponse {

    private String taskId;
    private String message;
}
