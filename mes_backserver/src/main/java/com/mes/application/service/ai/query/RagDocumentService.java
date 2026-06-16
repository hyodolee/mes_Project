package com.mes.application.service.ai.query;

import com.mes.domain.ai.dto.RagDocumentDto;
import com.mes.domain.ai.dto.RagDocumentUploadResponse;
import com.mes.infra.persistence.mybatis.mapper.ai.RagDocumentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class RagDocumentService {

    private static final int CHUNK_SIZE = 1200;
    private static final int CHUNK_OVERLAP = 180;
    // scl/st/l5x/awl: PLC 프로그램 소스/익스포트 — 모두 텍스트로 취급
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("md", "txt", "pdf", "docx", "xlsx", "scl", "st", "l5x", "awl");

    private final RagDocumentMapper ragDocumentMapper;
    private final VectorStore vectorStore;
    private final Path uploadDir;

    public RagDocumentService(
            RagDocumentMapper ragDocumentMapper,
            VectorStore vectorStore,
            @Value("${ai.rag.upload-dir:../tmp/rag-uploads}") String uploadDir
    ) {
        this.ragDocumentMapper = ragDocumentMapper;
        this.vectorStore = vectorStore;
        this.uploadDir = Path.of(uploadDir).normalize();
    }

    public List<RagDocumentDto> findAll() {
        return ragDocumentMapper.findAll();
    }

    public RagDocumentUploadResponse upload(MultipartFile file, String documentCategory, String documentType, String tags) {
        validate(file, documentCategory, documentType);

        String originalFileName = sanitizeFileName(file.getOriginalFilename());
        String extension = extensionOf(originalFileName);
        String storedFileName = UUID.randomUUID() + "." + extension;
        Path storedPath = uploadDir.resolve(storedFileName).normalize();

        try {
            Files.createDirectories(uploadDir);
            file.transferTo(storedPath);
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장에 실패했습니다.", e);
        }

        RagDocumentDto document = new RagDocumentDto();
        document.setOriginalFileName(originalFileName);
        document.setStoredFileName(storedFileName);
        document.setStoredFilePath(storedPath.toString());
        document.setContentType(file.getContentType());
        document.setDocumentCategory(documentCategory.trim());
        document.setDocumentType(documentType.trim());
        document.setTags(normalizeOptional(tags));
        document.setStatus("UPLOADED");
        document.setChunkCount(0);
        ragDocumentMapper.insert(document);

        int chunkCount = indexDocument(document);
        return new RagDocumentUploadResponse(
                document.getDocumentId(),
                document.getOriginalFileName(),
                document.getDocumentCategory(),
                document.getDocumentType(),
                "COMPLETED",
                chunkCount
        );
    }

    public RagDocumentUploadResponse reindex(Long documentId) {
        RagDocumentDto document = requireDocument(documentId);
        deleteFromVectorStore(documentId);
        int chunkCount = indexDocument(document);
        return new RagDocumentUploadResponse(
                document.getDocumentId(),
                document.getOriginalFileName(),
                document.getDocumentCategory(),
                document.getDocumentType(),
                "COMPLETED",
                chunkCount
        );
    }

    public List<RagDocumentUploadResponse> reindexAll() {
        return ragDocumentMapper.findAll().stream()
                .map(document -> reindex(document.getDocumentId()))
                .toList();
    }

    public void delete(Long documentId) {
        RagDocumentDto document = requireDocument(documentId);
        deleteFromVectorStore(documentId);
        ragDocumentMapper.updateStatus(document.getDocumentId(), "DELETED", 0, null);
    }

    private int indexDocument(RagDocumentDto document) {
        ragDocumentMapper.updateStatus(document.getDocumentId(), "PROCESSING", 0, null);

        try {
            String text = extractText(Path.of(document.getStoredFilePath()), document.getOriginalFileName());
            List<Document> chunks = createChunks(document, text);
            if (chunks.isEmpty()) {
                ragDocumentMapper.updateStatus(document.getDocumentId(), "FAILED", 0, "문서에서 추출된 텍스트가 없습니다.");
                return 0;
            }

            vectorStore.add(chunks);
            ragDocumentMapper.updateStatus(document.getDocumentId(), "COMPLETED", chunks.size(), null);
            log.info("[RAG-UPLOAD] 문서 적재 완료: {} ({} chunks)", document.getOriginalFileName(), chunks.size());
            return chunks.size();
        } catch (Exception e) {
            String message = e.getMessage() == null ? "문서 처리 중 오류가 발생했습니다." : e.getMessage();
            ragDocumentMapper.updateStatus(document.getDocumentId(), "FAILED", 0, message);
            log.warn("[RAG-UPLOAD] 문서 적재 실패: {} ({})", document.getOriginalFileName(), message);
            throw new IllegalStateException(message, e);
        }
    }

    private List<Document> createChunks(RagDocumentDto document, String rawText) {
        String text = normalizeText(rawText);
        if (text.isBlank()) {
            return List.of();
        }

        List<Document> chunks = new ArrayList<>();
        int index = 0;
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            String chunk = text.substring(start, end).trim();
            if (!chunk.isBlank()) {
                chunks.add(new Document(chunk, Map.of(
                        "source", "upload",
                        "documentId", String.valueOf(document.getDocumentId()),
                        "document", document.getOriginalFileName(),
                        "documentCategory", document.getDocumentCategory(),
                        "documentType", document.getDocumentType(),
                        "tags", document.getTags() == null ? "" : document.getTags(),
                        "chunkIndex", index
                )));
                index++;
            }
            if (end == text.length()) {
                break;
            }
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }

        return chunks;
    }

    private String extractText(Path path, String fileName) throws IOException {
        String extension = extensionOf(fileName);
        return switch (extension) {
            case "md", "txt", "scl", "st", "l5x", "awl" -> Files.readString(path, StandardCharsets.UTF_8);
            case "pdf" -> extractPdf(path);
            case "docx" -> extractDocx(path);
            case "xlsx" -> extractXlsx(path);
            default -> throw new IllegalArgumentException("지원하지 않는 파일 형식입니다: " + extension);
        };
    }

    private String extractPdf(Path path) throws IOException {
        try {
            Class<?> loaderClass = Class.forName("org.apache.pdfbox.Loader");
            Class<?> pdfTextStripperClass = Class.forName("org.apache.pdfbox.text.PDFTextStripper");
            Method loadPdf = loaderClass.getMethod("loadPDF", java.io.File.class);
            Object document = loadPdf.invoke(null, path.toFile());
            try {
                Object stripper = pdfTextStripperClass.getConstructor().newInstance();
                return (String) pdfTextStripperClass.getMethod("getText", Class.forName("org.apache.pdfbox.pdmodel.PDDocument"))
                        .invoke(stripper, document);
            } finally {
                document.getClass().getMethod("close").invoke(document);
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("PDF 파일을 읽으려면 PDFBox 라이브러리가 필요합니다. Gradle 의존성을 새로고침해 주세요.", e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("PDF 파일에서 텍스트를 추출하지 못했습니다.", e);
        }
    }

    private String extractDocx(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            Class<?> documentClass = Class.forName("org.apache.poi.xwpf.usermodel.XWPFDocument");
            Class<?> extractorClass = Class.forName("org.apache.poi.xwpf.extractor.XWPFWordExtractor");
            Object document = documentClass.getConstructor(InputStream.class).newInstance(in);
            try {
                Object extractor = extractorClass.getConstructor(documentClass).newInstance(document);
                try {
                    return (String) extractorClass.getMethod("getText").invoke(extractor);
                } finally {
                    extractorClass.getMethod("close").invoke(extractor);
                }
            } finally {
                documentClass.getMethod("close").invoke(document);
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("DOCX 파일을 읽으려면 Apache POI 라이브러리가 필요합니다. Gradle 의존성을 새로고침해 주세요.", e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("DOCX 파일에서 텍스트를 추출하지 못했습니다.", e);
        }
    }

    private String extractXlsx(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            Class<?> workbookFactoryClass = Class.forName("org.apache.poi.ss.usermodel.WorkbookFactory");
            Class<?> dataFormatterClass = Class.forName("org.apache.poi.ss.usermodel.DataFormatter");
            Object workbook = workbookFactoryClass.getMethod("create", InputStream.class).invoke(null, in);
            Object formatter = dataFormatterClass.getConstructor(Locale.class).newInstance(Locale.KOREA);

            try {
                StringBuilder text = new StringBuilder();
                Object sheets = workbook.getClass().getMethod("sheetIterator").invoke(workbook);
                Method hasNext = sheets.getClass().getMethod("hasNext");
                Method next = sheets.getClass().getMethod("next");

                while ((boolean) hasNext.invoke(sheets)) {
                    Object sheet = next.invoke(sheets);
                    text.append("# ").append(sheet.getClass().getMethod("getSheetName").invoke(sheet)).append("\n");
                    Object rows = sheet.getClass().getMethod("rowIterator").invoke(sheet);
                    Method rowHasNext = rows.getClass().getMethod("hasNext");
                    Method rowNext = rows.getClass().getMethod("next");

                    while ((boolean) rowHasNext.invoke(rows)) {
                        Object row = rowNext.invoke(rows);
                        Object cells = row.getClass().getMethod("cellIterator").invoke(row);
                        Method cellHasNext = cells.getClass().getMethod("hasNext");
                        Method cellNext = cells.getClass().getMethod("next");

                        while ((boolean) cellHasNext.invoke(cells)) {
                            Object cell = cellNext.invoke(cells);
                            String value = (String) dataFormatterClass
                                    .getMethod("formatCellValue", Class.forName("org.apache.poi.ss.usermodel.Cell"))
                                    .invoke(formatter, cell);
                            if (!value.isBlank()) {
                                text.append(value).append('\t');
                            }
                        }
                        text.append('\n');
                    }
                    text.append('\n');
                }
                return text.toString();
            } finally {
                workbook.getClass().getMethod("close").invoke(workbook);
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("XLSX 파일을 읽으려면 Apache POI 라이브러리가 필요합니다. Gradle 의존성을 새로고침해 주세요.", e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("XLSX 파일에서 텍스트를 추출하지 못했습니다.", e);
        }
    }

    private void deleteFromVectorStore(Long documentId) {
        try {
            var b = new FilterExpressionBuilder();
            vectorStore.delete(b.eq("documentId", String.valueOf(documentId)).build());
        } catch (Exception e) {
            log.warn("[RAG-UPLOAD] Chroma 문서 삭제 실패: documentId={} ({})", documentId, e.getMessage());
        }
    }

    private RagDocumentDto requireDocument(Long documentId) {
        RagDocumentDto document = ragDocumentMapper.findById(documentId);
        if (document == null || "DELETED".equals(document.getStatus())) {
            throw new IllegalArgumentException("RAG 문서를 찾을 수 없습니다.");
        }
        return document;
    }

    private void validate(MultipartFile file, String documentCategory, String documentType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일을 선택해 주세요.");
        }
        if (documentCategory == null || documentCategory.isBlank()) {
            throw new IllegalArgumentException("문서 분류를 선택해 주세요.");
        }
        if (documentType == null || documentType.isBlank()) {
            throw new IllegalArgumentException("세부 문서 유형을 입력해 주세요.");
        }

        String fileName = sanitizeFileName(file.getOriginalFilename());
        String extension = extensionOf(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. md, txt, pdf, docx, xlsx, scl, st, l5x, awl만 업로드할 수 있습니다.");
        }
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String sanitizeFileName(String value) {
        String fileName = value == null || value.isBlank() ? "document.txt" : Path.of(value).getFileName().toString();
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
