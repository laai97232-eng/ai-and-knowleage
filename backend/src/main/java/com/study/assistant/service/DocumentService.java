package com.study.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.assistant.common.BizException;
import com.study.assistant.config.AppProperties;
import com.study.assistant.entity.Course;
import com.study.assistant.entity.KnowledgePoint;
import com.study.assistant.entity.LearningDocument;
import com.study.assistant.mapper.CourseMapper;
import com.study.assistant.mapper.KnowledgePointMapper;
import com.study.assistant.mapper.LearningDocumentMapper;
import com.study.assistant.model.ApiModels.DocumentUpdate;
import com.study.assistant.model.ApiModels.DocumentView;
import com.study.assistant.model.ApiModels.PageResult;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {
    private static final Set<String> ALLOWED = Set.of("pdf", "doc", "docx", "md", "markdown", "txt", "ppt", "pptx");
    private final LearningDocumentMapper documentMapper;
    private final CourseMapper courseMapper;
    private final KnowledgePointMapper pointMapper;
    private final AppProperties props;
    private final IndexService indexService;

    public DocumentService(LearningDocumentMapper documentMapper, CourseMapper courseMapper,
                           KnowledgePointMapper pointMapper, AppProperties props, @Lazy IndexService indexService) {
        this.documentMapper = documentMapper;
        this.courseMapper = courseMapper;
        this.pointMapper = pointMapper;
        this.props = props;
        this.indexService = indexService;
    }

    public PageResult<DocumentView> page(boolean admin, Long courseId, Long knowledgePointId, String keyword, long page, long size) {
        LambdaQueryWrapper<LearningDocument> query = new LambdaQueryWrapper<>();
        if (!admin) {
            query.eq(LearningDocument::getStatus, 1);
        }
        if (courseId != null) {
            query.eq(LearningDocument::getCourseId, courseId);
        }
        if (knowledgePointId != null) {
            query.eq(LearningDocument::getKnowledgePointId, knowledgePointId);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.like(LearningDocument::getTitle, keyword.trim());
        }
        query.orderByDesc(LearningDocument::getId);
        Page<LearningDocument> result = documentMapper.selectPage(new Page<>(page, size), query);
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(),
                result.getRecords().stream().map(this::toView).toList());
    }

    @Transactional
    public DocumentView upload(Long uploaderId, Long courseId, Long knowledgePointId, String title, String category, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.bad("请选择文件");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BizException.bad("课程不存在");
        }
        if (knowledgePointId != null) {
            KnowledgePoint point = pointMapper.selectById(knowledgePointId);
            if (point == null || !courseId.equals(point.getCourseId())) {
                throw BizException.bad("知识点不属于该课程");
            }
        }
        String original = file.getOriginalFilename() == null ? "file.txt" : file.getOriginalFilename();
        String ext = extension(original);
        if (!ALLOWED.contains(ext)) {
            throw BizException.bad("仅支持 PDF、Word、Markdown、TXT、PPT");
        }
        String stored = UUID.randomUUID() + "." + ext;
        Path target = props.uploadRoot().resolve("materials").resolve(stored);
        try {
            Files.copy(file.getInputStream(), target);
        } catch (IOException e) {
            throw BizException.bad("文件保存失败");
        }
        LearningDocument document = new LearningDocument();
        document.setCourseId(courseId);
        document.setKnowledgePointId(knowledgePointId);
        document.setTitle(title == null || title.isBlank() ? original : title.trim());
        document.setFileName(original);
        document.setFilePath("materials/" + stored);
        document.setFileType(ext);
        document.setCategory(category == null || category.isBlank() ? "其他" : category.trim());
        document.setStatus(1);
        document.setUploaderId(uploaderId);
        documentMapper.insert(document);
        indexService.indexDocument(document);
        return toView(document);
    }

    @Transactional
    public void update(Long id, DocumentUpdate request) {
        LearningDocument document = require(id);
        if (request.title() != null && !request.title().isBlank()) {
            document.setTitle(request.title().trim());
        }
        if (request.category() != null) {
            document.setCategory(request.category());
        }
        if (request.status() != null) {
            document.setStatus(request.status());
        }
        if (request.knowledgePointId() != null) {
            KnowledgePoint point = pointMapper.selectById(request.knowledgePointId());
            if (point == null) {
                throw BizException.bad("知识点不存在");
            }
            document.setKnowledgePointId(point.getId());
            document.setCourseId(point.getCourseId());
        }
        documentMapper.updateById(document);
        indexService.indexDocument(document);
    }

    @Transactional
    public void delete(Long id) {
        LearningDocument document = require(id);
        deleteFile(document.getFilePath());
        indexService.removeDocument(id);
        documentMapper.deleteById(id);
    }

    public String readText(Long id, boolean admin) {
        LearningDocument document = requireVisible(id, admin);
        String type = document.getFileType() == null ? "" : document.getFileType().toLowerCase(Locale.ROOT);
        if (!Set.of("txt", "md", "markdown").contains(type)) {
            throw BizException.bad("该文件请下载后查看");
        }
        try {
            return Files.readString(resolve(document.getFilePath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw BizException.bad("文件不存在或无法读取");
        }
    }

    public LearningDocument requireVisible(Long id, boolean admin) {
        LearningDocument document = require(id);
        if (!admin && (document.getStatus() == null || document.getStatus() != 1)) {
            throw BizException.bad("资料不存在");
        }
        return document;
    }

    public Path resolve(String relative) {
        if (relative == null || relative.isBlank()) {
            throw BizException.bad("文件不存在");
        }
        Path root = props.uploadRoot();
        Path file = root.resolve(relative).normalize();
        if (!file.startsWith(root)) {
            throw BizException.bad("非法路径");
        }
        return file;
    }

    public void deleteFile(String relative) {
        if (relative == null || relative.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(relative));
        } catch (Exception ignored) {
            // 文件缺失时仍允许删除数据库记录
        }
    }

    private LearningDocument require(Long id) {
        LearningDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw BizException.bad("资料不存在");
        }
        return document;
    }

    private DocumentView toView(LearningDocument document) {
        Course course = document.getCourseId() == null ? null : courseMapper.selectById(document.getCourseId());
        KnowledgePoint point = document.getKnowledgePointId() == null ? null : pointMapper.selectById(document.getKnowledgePointId());
        return new DocumentView(document.getId(), document.getTitle(), document.getFileName(), document.getFileType(),
                document.getCategory(), document.getStatus(), document.getCourseId(), course == null ? "" : course.getName(),
                document.getKnowledgePointId(), point == null ? "" : point.getName(), document.getCreatedAt());
    }

    private String extension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
