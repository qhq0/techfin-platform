package com.ccb.techfin.model.sxd.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttachmentUploadResponse {

    private boolean success;
    private String code;
    private String message;
    private String data;
}
