package com.study.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.assistant.config.AppProperties;
import com.study.assistant.entity.DocumentChunk;
import com.study.assistant.entity.KnowledgePoint;
import com.study.assistant.entity.LearningDocument;
import com.study.assistant.mapper.DocumentChunkMapper;
import com.study.assistant.mapper.KnowledgePointMapper;
import com.study.assistant.mapper.LearningDocumentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Order(10)
public class IndexService implements ApplicationRunner {
    private static final int CHUNK_SIZE = 480;
    private static final int OVERLAP = 80;

    private final DocumentChunkMapper chunkMapper;
    private final LearningDocumentMapper documentMapper;
    private final KnowledgePointMapper pointMapper;
    private final TextExtractService textExtractService;
    private final EmbeddingService embeddingService;
    private final AppProperties props;
    private final ObjectMapper objectMapper;

    public IndexService(DocumentChunkMapper chunkMapper, LearningDocumentMapper documentMapper,
                        KnowledgePointMapper pointMapper, TextExtractService textExtractService,
                        EmbeddingService embeddingService, AppProperties props, ObjectMapper objectMapper) {
        this.chunkMapper = chunkMapper;
        this.documentMapper = documentMapper;
        this.pointMapper = pointMapper;
        this.textExtractService = textExtractService;
        this.embeddingService = embeddingService;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long count = chunkMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        int chunks = rebuild();
        log.info("已为课程资料建立向量索引，片段 {}", chunks);
    }

    @Transactional
    public int rebuild() {
        chunkMapper.delete(new LambdaQueryWrapper<DocumentChunk>().ge(DocumentChunk::getId, 0));
        int total = 0;
        for (LearningDocument document : documentMapper.selectList(new LambdaQueryWrapper<LearningDocument>().eq(LearningDocument::getStatus, 1))) {
            total += indexDocument(document);
        }
        for (KnowledgePoint point : pointMapper.selectList(null)) {
            total += indexKnowledge(point);
        }
        return total;
    }

    @Transactional
    public int indexDocument(LearningDocument document) {
        chunkMapper.delete(new LambdaQueryWrapper<DocumentChunk>().eq(DocumentChunk::getDocumentId, document.getId()));
        if (document.getStatus() == null || document.getStatus() != 1) {
            return 0;
        }
        Path path = props.uploadRoot().resolve(document.getFilePath() == null ? "" : document.getFilePath()).normalize();
        if (!path.startsWith(props.uploadRoot())) {
            return 0;
        }
        String text = textExtractService.extract(path, document.getFileType());
        if (text.isBlank()) {
            return 0;
        }
        List<String> chunks = split(document.getTitle() + "\n" + text);
        saveChunks(chunks, document.getTitle(), document.getId(), document.getCourseId(), document.getKnowledgePointId());
        return chunks.size();
    }

    @Transactional
    public int indexKnowledge(KnowledgePoint point) {
        chunkMapper.delete(new LambdaQueryWrapper<DocumentChunk>()
                .isNull(DocumentChunk::getDocumentId)
                .eq(DocumentChunk::getKnowledgePointId, point.getId()));
        if (point.getDescription() == null || point.getDescription().isBlank()) {
            return 0;
        }
        String title = "知识点：" + point.getName();
        List<String> chunks = split(title + "\n" + point.getDescription());
        saveChunks(chunks, title, null, point.getCourseId(), point.getId());
        return chunks.size();
    }

    public void removeDocument(Long documentId) {
        chunkMapper.delete(new LambdaQueryWrapper<DocumentChunk>().eq(DocumentChunk::getDocumentId, documentId));
    }

    public void removeKnowledge(Long pointId) {
        chunkMapper.delete(new LambdaQueryWrapper<DocumentChunk>().eq(DocumentChunk::getKnowledgePointId, pointId));
    }

    public void removeCourse(Long courseId) {
        chunkMapper.delete(new LambdaQueryWrapper<DocumentChunk>().eq(DocumentChunk::getCourseId, courseId));
    }

    public long chunkCount() {
        Long count = chunkMapper.selectCount(null);
        return count == null ? 0 : count;
    }

    private void saveChunks(List<String> chunks, String title, Long documentId, Long courseId, Long pointId) {
        int index = 0;
        for (String chunk : chunks) {
            DocumentChunk row = new DocumentChunk();
            row.setDocumentId(documentId);
            row.setCourseId(courseId);
            row.setKnowledgePointId(pointId);
            row.setTitle(title);
            row.setChunkIndex(index++);
            row.setContent(chunk);
            row.setEmbeddingJson(writeVector(embeddingService.embed(chunk)));
            row.setCreatedAt(LocalDateTime.now());
            chunkMapper.insert(row);
        }
    }

    private List<String> split(String text) {
        String normalized = text.replace("\r\n", "\n").replaceAll("[ \\t]+", " ").trim();
        List<String> chunks = new ArrayList<>();
        if (normalized.length() <= CHUNK_SIZE) {
            chunks.add(normalized);
            return chunks;
        }
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + CHUNK_SIZE);
            if (end < normalized.length()) {
                int breakAt = normalized.lastIndexOf('\n', end);
                if (breakAt > start + CHUNK_SIZE / 2) {
                    end = breakAt;
                }
            }
            String piece = normalized.substring(start, end).trim();
            if (!piece.isBlank()) {
                chunks.add(piece);
            }
            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(end - OVERLAP, start + 1);
        }
        return chunks;
    }

    private String writeVector(float[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (Exception ex) {
            return "[]";
        }
    }
}
