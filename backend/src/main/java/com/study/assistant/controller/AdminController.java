package com.study.assistant.controller;

import com.study.assistant.common.Auth;
import com.study.assistant.common.R;
import com.study.assistant.entity.Chapter;
import com.study.assistant.entity.Course;
import com.study.assistant.entity.KnowledgePoint;
import com.study.assistant.model.ApiModels.AdminStats;
import com.study.assistant.model.ApiModels.ChapterSave;
import com.study.assistant.model.ApiModels.CourseCard;
import com.study.assistant.model.ApiModels.CourseDetail;
import com.study.assistant.model.ApiModels.CourseSave;
import com.study.assistant.model.ApiModels.DocumentUpdate;
import com.study.assistant.model.ApiModels.DocumentView;
import com.study.assistant.model.ApiModels.KnowledgeOption;
import com.study.assistant.model.ApiModels.KnowledgeSave;
import com.study.assistant.model.ApiModels.PageResult;
import com.study.assistant.model.ApiModels.QuestionAdmin;
import com.study.assistant.model.ApiModels.QuestionSave;
import com.study.assistant.model.ApiModels.RelationSave;
import com.study.assistant.model.ApiModels.RelationView;
import com.study.assistant.model.ApiModels.UserAdmin;
import com.study.assistant.model.ApiModels.UserUpdate;
import com.study.assistant.service.AdminService;
import com.study.assistant.service.CourseService;
import com.study.assistant.service.DocumentService;
import com.study.assistant.service.IndexService;
import com.study.assistant.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;
    private final CourseService courseService;
    private final DocumentService documentService;
    private final QuestionService questionService;
    private final IndexService indexService;

    public AdminController(AdminService adminService, CourseService courseService,
                           DocumentService documentService, QuestionService questionService, IndexService indexService) {
        this.adminService = adminService;
        this.courseService = courseService;
        this.documentService = documentService;
        this.questionService = questionService;
        this.indexService = indexService;
    }

    @PostMapping("/index/rebuild")
    public R<Integer> rebuildIndex() {
        return R.ok(indexService.rebuild());
    }

    @GetMapping("/stats")
    public R<AdminStats> stats() {
        return R.ok(adminService.stats());
    }

    @GetMapping("/users")
    public R<PageResult<UserAdmin>> users(@RequestParam(required = false) String keyword,
                                          @RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size) {
        return R.ok(adminService.users(keyword, page, size));
    }

    @PutMapping("/users/{id}")
    public R<Void> updateUser(@PathVariable Long id, @RequestBody UserUpdate request) {
        adminService.updateUser(Auth.require().getId(), id, request);
        return R.ok(null);
    }

    @DeleteMapping("/users/{id}")
    public R<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(Auth.require().getId(), id);
        return R.ok(null);
    }

    @GetMapping("/courses")
    public R<List<CourseCard>> courses() {
        return R.ok(courseService.listForAdmin());
    }

    @GetMapping("/courses/{id}")
    public R<CourseDetail> course(@PathVariable Long id) {
        return R.ok(courseService.detail(id, Auth.require().getId(), true));
    }

    @PostMapping("/courses")
    public R<Course> createCourse(@Valid @RequestBody CourseSave request) {
        return R.ok(courseService.createCourse(request));
    }

    @PutMapping("/courses/{id}")
    public R<Void> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseSave request) {
        courseService.updateCourse(id, request);
        return R.ok(null);
    }

    @DeleteMapping("/courses/{id}")
    public R<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return R.ok(null);
    }

    @PostMapping("/courses/{courseId}/chapters")
    public R<Chapter> createChapter(@PathVariable Long courseId, @Valid @RequestBody ChapterSave request) {
        return R.ok(courseService.createChapter(courseId, request));
    }

    @PutMapping("/chapters/{id}")
    public R<Void> updateChapter(@PathVariable Long id, @Valid @RequestBody ChapterSave request) {
        courseService.updateChapter(id, request);
        return R.ok(null);
    }

    @DeleteMapping("/chapters/{id}")
    public R<Void> deleteChapter(@PathVariable Long id) {
        courseService.deleteChapter(id);
        return R.ok(null);
    }

    @PostMapping("/chapters/{chapterId}/knowledge")
    public R<KnowledgePoint> createKnowledge(@PathVariable Long chapterId, @Valid @RequestBody KnowledgeSave request) {
        return R.ok(courseService.createKnowledge(chapterId, request));
    }

    @PutMapping("/knowledge/{id}")
    public R<Void> updateKnowledge(@PathVariable Long id, @Valid @RequestBody KnowledgeSave request) {
        courseService.updateKnowledge(id, request);
        return R.ok(null);
    }

    @DeleteMapping("/knowledge/{id}")
    public R<Void> deleteKnowledge(@PathVariable Long id) {
        courseService.deleteKnowledge(id);
        return R.ok(null);
    }

    @GetMapping("/knowledge-points")
    public R<List<KnowledgeOption>> knowledgeOptions() {
        return R.ok(courseService.knowledgeOptions());
    }

    @GetMapping("/relations")
    public R<List<RelationView>> relations() {
        return R.ok(courseService.relations());
    }

    @PostMapping("/relations")
    public R<Void> createRelation(@Valid @RequestBody RelationSave request) {
        courseService.createRelation(request);
        return R.ok(null);
    }

    @DeleteMapping("/relations/{id}")
    public R<Void> deleteRelation(@PathVariable Long id) {
        courseService.deleteRelation(id);
        return R.ok(null);
    }

    @GetMapping("/documents")
    public R<PageResult<DocumentView>> documents(@RequestParam(required = false) Long courseId,
                                                 @RequestParam(required = false) Long knowledgePointId,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size) {
        return R.ok(documentService.page(true, courseId, knowledgePointId, keyword, page, size));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<DocumentView> upload(@RequestParam Long courseId,
                                  @RequestParam(required = false) Long knowledgePointId,
                                  @RequestParam(required = false) String title,
                                  @RequestParam(required = false) String category,
                                  @RequestPart("file") MultipartFile file) {
        return R.ok(documentService.upload(Auth.require().getId(), courseId, knowledgePointId, title, category, file));
    }

    @PutMapping("/documents/{id}")
    public R<Void> updateDocument(@PathVariable Long id, @RequestBody DocumentUpdate request) {
        documentService.update(id, request);
        return R.ok(null);
    }

    @DeleteMapping("/documents/{id}")
    public R<Void> deleteDocument(@PathVariable Long id) {
        documentService.delete(id);
        return R.ok(null);
    }

    @GetMapping("/questions")
    public R<PageResult<QuestionAdmin>> questions(@RequestParam(required = false) Long courseId,
                                                  @RequestParam(required = false) Long knowledgePointId,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size) {
        return R.ok(questionService.adminPage(courseId, knowledgePointId, keyword, page, size));
    }

    @PostMapping("/questions")
    public R<Void> createQuestion(@Valid @RequestBody QuestionSave request) {
        questionService.save(request, null);
        return R.ok(null);
    }

    @PutMapping("/questions/{id}")
    public R<Void> updateQuestion(@PathVariable Long id, @Valid @RequestBody QuestionSave request) {
        questionService.save(request, id);
        return R.ok(null);
    }

    @DeleteMapping("/questions/{id}")
    public R<Void> deleteQuestion(@PathVariable Long id) {
        questionService.delete(id);
        return R.ok(null);
    }
}
