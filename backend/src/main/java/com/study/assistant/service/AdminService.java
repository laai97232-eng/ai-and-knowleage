package com.study.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.assistant.common.BizException;
import com.study.assistant.entity.AnswerRecord;
import com.study.assistant.entity.LearningProgress;
import com.study.assistant.entity.StudyPlan;
import com.study.assistant.entity.StudyRecord;
import com.study.assistant.entity.User;
import com.study.assistant.entity.WrongQuestion;
import com.study.assistant.mapper.AnswerRecordMapper;
import com.study.assistant.mapper.CourseMapper;
import com.study.assistant.mapper.KnowledgePointMapper;
import com.study.assistant.mapper.LearningDocumentMapper;
import com.study.assistant.mapper.LearningProgressMapper;
import com.study.assistant.mapper.QuestionMapper;
import com.study.assistant.mapper.StudyPlanItemMapper;
import com.study.assistant.mapper.StudyPlanMapper;
import com.study.assistant.mapper.StudyRecordMapper;
import com.study.assistant.mapper.UserMapper;
import com.study.assistant.mapper.WrongQuestionMapper;
import com.study.assistant.model.ApiModels.AdminStats;
import com.study.assistant.model.ApiModels.PageResult;
import com.study.assistant.model.ApiModels.UserAdmin;
import com.study.assistant.model.ApiModels.UserUpdate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final KnowledgePointMapper pointMapper;
    private final QuestionMapper questionMapper;
    private final LearningDocumentMapper documentMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final StudyRecordMapper studyRecordMapper;
    private final StudyPlanMapper planMapper;
    private final StudyPlanItemMapper planItemMapper;
    private final LearningProgressMapper progressMapper;
    private final StudyService studyService;

    public AdminService(UserMapper userMapper, CourseMapper courseMapper, KnowledgePointMapper pointMapper,
                        QuestionMapper questionMapper, LearningDocumentMapper documentMapper,
                        AnswerRecordMapper answerRecordMapper, WrongQuestionMapper wrongQuestionMapper,
                        StudyRecordMapper studyRecordMapper, StudyPlanMapper planMapper, StudyPlanItemMapper planItemMapper,
                        LearningProgressMapper progressMapper, StudyService studyService) {
        this.userMapper = userMapper;
        this.courseMapper = courseMapper;
        this.pointMapper = pointMapper;
        this.questionMapper = questionMapper;
        this.documentMapper = documentMapper;
        this.answerRecordMapper = answerRecordMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.studyRecordMapper = studyRecordMapper;
        this.planMapper = planMapper;
        this.planItemMapper = planItemMapper;
        this.progressMapper = progressMapper;
        this.studyService = studyService;
    }

    public PageResult<UserAdmin> users(String keyword, long page, long size) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>().orderByDesc(User::getId);
        if (keyword != null && !keyword.isBlank()) {
            query.like(User::getUsername, keyword.trim());
        }
        Page<User> result = userMapper.selectPage(new Page<>(page, size), query);
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(),
                result.getRecords().stream()
                        .map(user -> new UserAdmin(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.getStatus(), user.getCreatedAt()))
                        .toList());
    }

    public void updateUser(Long operatorId, Long id, UserUpdate request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.bad("用户不存在");
        }
        if (operatorId.equals(id) && request.role() != null && !request.role().equals(user.getRole())) {
            throw BizException.bad("不能修改自己的角色");
        }
        if (operatorId.equals(id) && request.status() != null && request.status() != 1) {
            throw BizException.bad("不能禁用自己");
        }
        if (request.email() != null) {
            user.setEmail(request.email().isBlank() ? null : request.email().trim());
        }
        if (request.role() != null) {
            if (!"ADMIN".equals(request.role()) && !"STUDENT".equals(request.role())) {
                throw BizException.bad("角色不正确");
            }
            user.setRole(request.role());
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        userMapper.updateById(user);
    }

    @Transactional
    public void deleteUser(Long operatorId, Long id) {
        if (operatorId.equals(id)) {
            throw BizException.bad("不能删除自己");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.bad("用户不存在");
        }
        answerRecordMapper.delete(new LambdaQueryWrapper<AnswerRecord>().eq(AnswerRecord::getUserId, id));
        wrongQuestionMapper.delete(new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getUserId, id));
        studyRecordMapper.delete(new LambdaQueryWrapper<StudyRecord>().eq(StudyRecord::getUserId, id));
        progressMapper.delete(new LambdaQueryWrapper<LearningProgress>().eq(LearningProgress::getUserId, id));
        planMapper.selectList(new LambdaQueryWrapper<StudyPlan>().eq(StudyPlan::getUserId, id)).forEach(plan ->
                planItemMapper.delete(new LambdaQueryWrapper<com.study.assistant.entity.StudyPlanItem>().eq(com.study.assistant.entity.StudyPlanItem::getPlanId, plan.getId())));
        planMapper.delete(new LambdaQueryWrapper<StudyPlan>().eq(StudyPlan::getUserId, id));
        userMapper.deleteById(id);
    }

    public AdminStats stats() {
        return new AdminStats(
                count(userMapper.selectCount(null)),
                count(courseMapper.selectCount(null)),
                count(pointMapper.selectCount(null)),
                count(questionMapper.selectCount(null)),
                count(documentMapper.selectCount(null)),
                studyService.totalMinutes(),
                count(answerRecordMapper.selectCount(null)));
    }

    private long count(Long value) {
        return value == null ? 0 : value;
    }
}
