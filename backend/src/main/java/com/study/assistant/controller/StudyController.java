package com.study.assistant.controller;

import com.study.assistant.common.Auth;
import com.study.assistant.common.R;
import com.study.assistant.model.ApiModels.GeneratePlanRequest;
import com.study.assistant.model.ApiModels.PageResult;
import com.study.assistant.model.ApiModels.PlanDetail;
import com.study.assistant.model.ApiModels.PlanView;
import com.study.assistant.model.ApiModels.RecordSave;
import com.study.assistant.model.ApiModels.RecordView;
import com.study.assistant.model.ApiModels.UpdatePlanItemRequest;
import com.study.assistant.service.StudyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class StudyController {
    private final StudyService studyService;

    public StudyController(StudyService studyService) {
        this.studyService = studyService;
    }

    @GetMapping("/study-records")
    public R<PageResult<RecordView>> records(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size) {
        return R.ok(studyService.records(Auth.require().getId(), page, size));
    }

    @PostMapping("/study-records")
    public R<Void> addRecord(@Valid @RequestBody RecordSave request) {
        studyService.addRecord(Auth.require().getId(), request);
        return R.ok(null);
    }

    @GetMapping("/study-plans")
    public R<List<PlanView>> plans() {
        return R.ok(studyService.plans(Auth.require().getId()));
    }

    @PostMapping("/study-plans/generate")
    public R<PlanDetail> generate(@Valid @RequestBody GeneratePlanRequest request) {
        return R.ok(studyService.generate(Auth.require().getId(), request));
    }

    @GetMapping("/study-plans/{id}")
    public R<PlanDetail> detail(@PathVariable Long id) {
        return R.ok(studyService.planDetail(Auth.require().getId(), id));
    }

    @DeleteMapping("/study-plans/{id}")
    public R<Void> delete(@PathVariable Long id) {
        studyService.deletePlan(Auth.require().getId(), id);
        return R.ok(null);
    }

    @PutMapping("/study-plan-items/{id}")
    public R<Void> updateItem(@PathVariable Long id, @RequestBody UpdatePlanItemRequest request) {
        studyService.updateItem(Auth.require().getId(), id, request);
        return R.ok(null);
    }
}
