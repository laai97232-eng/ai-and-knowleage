package com.study.assistant.controller;

import com.study.assistant.common.Auth;
import com.study.assistant.common.R;
import com.study.assistant.model.ApiModels.QuestionView;
import com.study.assistant.model.ApiModels.SubmitRequest;
import com.study.assistant.model.ApiModels.SubmitResult;
import com.study.assistant.model.ApiModels.WrongView;
import com.study.assistant.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class QuestionController {
    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/knowledge/{id}/questions")
    public R<List<QuestionView>> practice(@PathVariable Long id) {
        Auth.require();
        return R.ok(questionService.practice(id));
    }

    @PostMapping("/questions/{id}/submit")
    public R<SubmitResult> submit(@PathVariable Long id, @Valid @RequestBody SubmitRequest request) {
        return R.ok(questionService.submit(Auth.require().getId(), id, request.answer()));
    }

    @GetMapping("/wrong-questions")
    public R<List<WrongView>> wrong() {
        return R.ok(questionService.wrongList(Auth.require().getId()));
    }

    @DeleteMapping("/wrong-questions/{id}")
    public R<Void> removeWrong(@PathVariable Long id) {
        questionService.removeWrong(Auth.require().getId(), id);
        return R.ok(null);
    }
}
