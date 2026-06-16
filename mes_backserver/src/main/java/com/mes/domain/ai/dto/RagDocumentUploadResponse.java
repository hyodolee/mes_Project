package com.mes.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RagDocumentUploadResponse {

    private Long documentId;
    private String originalFileName;
    private String documentCategory;
    private String documentType;
    private String status;
    private int chunkCount;
}
