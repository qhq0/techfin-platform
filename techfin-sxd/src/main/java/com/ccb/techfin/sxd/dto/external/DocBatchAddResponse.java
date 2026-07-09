package com.ccb.techfin.sxd.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocBatchAddResponse {

    private boolean success;
    private String code;
    private String message;
    private DataBody data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataBody {
        private List<String> invalidDocNames;
        private List<DocInfo> docList;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DocInfo {
        private String id;
        private String docName;
        private String projectId;
        private String attId;
    }
}
