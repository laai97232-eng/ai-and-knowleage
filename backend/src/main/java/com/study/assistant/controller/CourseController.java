package com.study.assistant.controller;

import com.study.assistant.common.Auth;
import com.study.assistant.common.R;
import com.study.assistant.model.ApiModels.CourseCard;
import com.study.assistant.model.ApiModels.CourseDetail;
import com.study.assistant.model.ApiModels.KnowledgeDetail;
import com.study.assistant.service.CourseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CourseController {
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/courses")
    public R<List<CourseCard>> courses(@RequestParam(required = false) String keyword) {
        return R.ok(courseService.listForStudent(Auth.require().getId(), keyword));
    }

    @GetMapping("/courses/{id}")
    public R<CourseDetail> detail(@PathVariable Long id) {
        return R.ok(courseService.detail(id, Auth.require().getId(), false));
    }

    @GetMapping("/knowledge/{id}")
    public R<KnowledgeDetail> knowledge(@PathVariable Long id) {
        return R.ok(courseService.knowledgeDetail(id, Auth.require().getId()));
    }
}
