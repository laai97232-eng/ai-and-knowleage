package com.study.assistant.controller;

import com.study.assistant.common.Auth;
import com.study.assistant.common.R;
import com.study.assistant.entity.LearningDocument;
import com.study.assistant.model.ApiModels.DocumentView;
import com.study.assistant.model.ApiModels.PageResult;
import com.study.assistant.service.DocumentService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public R<PageResult<DocumentView>> list(@RequestParam(required = false) Long courseId,
                                            @RequestParam(required = false) Long knowledgePointId,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long size) {
        boolean admin = "ADMIN".equals(Auth.require().getRole());
        return R.ok(documentService.page(admin, courseId, knowledgePointId, keyword, page, size));
    }

    @GetMapping("/{id}/text")
    public R<String> text(@PathVariable Long id) {
        boolean admin = "ADMIN".equals(Auth.require().getRole());
        return R.ok(documentService.readText(id, admin));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> file(@PathVariable Long id) throws java.io.IOException {
        boolean admin = "ADMIN".equals(Auth.require().getRole());
        LearningDocument document = documentService.requireVisible(id, admin);
        Path path = documentService.resolve(document.getFilePath());
        String type = Files.probeContentType(path);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(document.getFileName() == null ? "file" : document.getFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(type == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : type))
                .body(Files.readAllBytes(path));
    }
}
