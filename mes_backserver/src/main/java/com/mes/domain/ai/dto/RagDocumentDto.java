package com.mes.domain.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class RagDocumentDto {

    private Long documentId;
    private String originalFileName;
    private String storedFileName;
    private String storedFilePath;
    private String contentType;
    private String documentCategory;
    private String documentType;
    private String tags;
    private String status;
    private Integer chunkCount;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
