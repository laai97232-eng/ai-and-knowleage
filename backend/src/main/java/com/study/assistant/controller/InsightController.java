package com.study.assistant.controller;

import com.study.assistant.common.Auth;
import com.study.assistant.common.R;
import com.study.assistant.service.InsightService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/insights")
public class InsightController {
    private final InsightService insightService;

    public InsightController(InsightService insightService) {
        this.insightService = insightService;
    }

    @GetMapping
    public R<Map<String, Object>> insights(@RequestParam Long courseId) {
        return R.ok(insightService.insights(Auth.require().getId(), courseId));
    }

    @GetMapping("/graph")
    public R<Map<String, Object>> graph(@RequestParam Long courseId) {
        return R.ok(insightService.graph(Auth.require().getId(), courseId));
    }
}
