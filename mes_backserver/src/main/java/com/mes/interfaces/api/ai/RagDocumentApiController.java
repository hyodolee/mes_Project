package com.mes.interfaces.api.ai;

import com.mes.application.service.ai.query.RagDocumentService;
import com.mes.domain.ai.dto.RagDocumentDto;
import com.mes.domain.ai.dto.RagDocumentUploadResponse;
import com.mes.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/rag/documents")
@RequiredArgsConstructor
public class RagDocumentApiController {

    private final RagDocumentService ragDocumentService;

    @GetMapping
    public ApiResponse<List<RagDocumentDto>> findAll() {
        return ApiResponse.ok(ragDocumentService.findAll());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<RagDocumentUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentCategory") String documentCategory,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "tags", required = false) String tags
    ) {
        return ApiResponse.ok(ragDocumentService.upload(file, documentCategory, documentType, tags));
    }

    @PostMapping("/{documentId}/reindex")
    public ApiResponse<RagDocumentUploadResponse> reindex(@PathVariable("documentId") Long documentId) {
        return ApiResponse.ok(ragDocumentService.reindex(documentId));
    }

    @PostMapping("/reindex-all")
    public ApiResponse<List<RagDocumentUploadResponse>> reindexAll() {
        return ApiResponse.ok(ragDocumentService.reindexAll());
    }

    @DeleteMapping("/{documentId}")
    public ApiResponse<Void> delete(@PathVariable("documentId") Long documentId) {
        ragDocumentService.delete(documentId);
        return ApiResponse.ok();
    }
}
