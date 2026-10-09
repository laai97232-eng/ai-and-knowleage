package com.study.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.assistant.common.BizException;
import com.study.assistant.entity.AnswerRecord;
import com.study.assistant.entity.Course;
import com.study.assistant.entity.KnowledgePoint;
import com.study.assistant.entity.Question;
import com.study.assistant.entity.WrongQuestion;
import com.study.assistant.mapper.AnswerRecordMapper;
import com.study.assistant.mapper.CourseMapper;
import com.study.assistant.mapper.KnowledgePointMapper;
import com.study.assistant.mapper.QuestionMapper;
import com.study.assistant.mapper.WrongQuestionMapper;
import com.study.assistant.model.ApiModels.PageResult;
import com.study.assistant.model.ApiModels.QuestionAdmin;
import com.study.assistant.model.ApiModels.QuestionSave;
import com.study.assistant.model.ApiModels.QuestionView;
import com.study.assistant.model.ApiModels.SubmitResult;
import com.study.assistant.model.ApiModels.WrongView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class QuestionService {
    private final QuestionMapper questionMapper;
    private final KnowledgePointMapper pointMapper;
    private final CourseMapper courseMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final ProgressService progressService;
    private final ObjectMapper objectMapper;

    public QuestionService(QuestionMapper questionMapper, KnowledgePointMapper pointMapper, CourseMapper courseMapper,
                           AnswerRecordMapper answerRecordMapper, WrongQuestionMapper wrongQuestionMapper,
                           ProgressService progressService, ObjectMapper objectMapper) {
        this.questionMapper = questionMapper;
        this.pointMapper = pointMapper;
        this.courseMapper = courseMapper;
        this.answerRecordMapper = answerRecordMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.progressService = progressService;
        this.objectMapper = objectMapper;
    }

    public List<QuestionView> practice(Long knowledgePointId) {
        KnowledgePoint point = pointMapper.selectById(knowledgePointId);
        if (point == null) {
            throw BizException.bad("知识点不存在");
        }
        return questionMapper.selectList(new LambdaQueryWrapper<Question>()
                        .eq(Question::getKnowledgePointId, knowledgePointId)
                        .orderByAsc(Question::getId))
                .stream()
                .map(q -> new QuestionView(q.getId(), q.getType(), q.getContent(), readOptions(q.getOptionsJson())))
                .toList();
    }

    @Transactional
    public SubmitResult submit(Long userId, Long questionId, String answer) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw BizException.bad("题目不存在");
        }
        String userAnswer = answer.trim().toUpperCase();
        boolean correct = userAnswer.equalsIgnoreCase(question.getAnswer());
        AnswerRecord record = new AnswerRecord();
        record.setUserId(userId);
        record.setQuestionId(questionId);
        record.setKnowledgePointId(question.getKnowledgePointId());
        record.setUserAnswer(userAnswer);
        record.setCorrect(correct ? 1 : 0);
        record.setCreatedAt(LocalDateTime.now());
        answerRecordMapper.insert(record);
        if (correct) {
            wrongQuestionMapper.delete(new LambdaQueryWrapper<WrongQuestion>()
                    .eq(WrongQuestion::getUserId, userId)
                    .eq(WrongQuestion::getQuestionId, questionId));
        } else {
            WrongQuestion wrong = wrongQuestionMapper.selectOne(new LambdaQueryWrapper<WrongQuestion>()
                    .eq(WrongQuestion::getUserId, userId)
                    .eq(WrongQuestion::getQuestionId, questionId));
            if (wrong == null) {
                wrong = new WrongQuestion();
                wrong.setUserId(userId);
                wrong.setQuestionId(questionId);
                wrong.setKnowledgePointId(question.getKnowledgePointId());
                wrong.setWrongCount(1);
                wrong.setLastAnswer(userAnswer);
                wrong.setLastWrongAt(LocalDateTime.now());
                wrongQuestionMapper.insert(wrong);
            } else {
                wrong.setWrongCount((wrong.getWrongCount() == null ? 0 : wrong.getWrongCount()) + 1);
                wrong.setLastAnswer(userAnswer);
                wrong.setLastWrongAt(LocalDateTime.now());
                wrongQuestionMapper.updateById(wrong);
            }
        }
        progressService.refreshByQuiz(userId, question.getCourseId(), question.getKnowledgePointId());
        return new SubmitResult(correct, question.getAnswer(), question.getAnalysis());
    }

    public List<WrongView> wrongList(Long userId) {
        return wrongQuestionMapper.selectList(new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .orderByDesc(WrongQuestion::getLastWrongAt))
                .stream()
                .map(wrong -> {
                    Question question = questionMapper.selectById(wrong.getQuestionId());
                    KnowledgePoint point = wrong.getKnowledgePointId() == null ? null : pointMapper.selectById(wrong.getKnowledgePointId());
                    return new WrongView(wrong.getId(), wrong.getQuestionId(),
                            question == null ? "题目已删除" : question.getContent(),
                            wrong.getKnowledgePointId(), point == null ? "" : point.getName(),
                            wrong.getWrongCount(), wrong.getLastAnswer(), wrong.getLastWrongAt());
                })
                .toList();
    }

    public void removeWrong(Long userId, Long id) {
        WrongQuestion wrong = wrongQuestionMapper.selectById(id);
        if (wrong == null || !userId.equals(wrong.getUserId())) {
            throw BizException.bad("错题不存在");
        }
        wrongQuestionMapper.deleteById(id);
    }

    public PageResult<QuestionAdmin> adminPage(Long courseId, Long knowledgePointId, String keyword, long page, long size) {
        LambdaQueryWrapper<Question> query = new LambdaQueryWrapper<>();
        if (courseId != null) {
            query.eq(Question::getCourseId, courseId);
        }
        if (knowledgePointId != null) {
            query.eq(Question::getKnowledgePointId, knowledgePointId);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.like(Question::getContent, keyword.trim());
        }
        query.orderByDesc(Question::getId);
        Page<Question> result = questionMapper.selectPage(new Page<>(page, size), query);
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(),
                result.getRecords().stream().map(this::toAdmin).toList());
    }

    @Transactional
    public void save(QuestionSave request, Long id) {
        KnowledgePoint point = pointMapper.selectById(request.knowledgePointId());
        if (point == null) {
            throw BizException.bad("知识点不存在");
        }
        String type = request.type().trim().toUpperCase();
        if (!"SINGLE".equals(type) && !"JUDGE".equals(type)) {
            throw BizException.bad("题型仅支持单选或判断");
        }
        List<String> options = request.options() == null ? List.of() : request.options().stream().filter(s -> s != null && !s.isBlank()).toList();
        if ("JUDGE".equals(type) && options.isEmpty()) {
            options = List.of("A. 正确", "B. 错误");
        }
        if (options.size() < 2) {
            throw BizException.bad("至少需要两个选项");
        }
        String answer = request.answer().trim().toUpperCase();
        Question question = id == null ? new Question() : questionMapper.selectById(id);
        if (id != null && question == null) {
            throw BizException.bad("题目不存在");
        }
        question.setCourseId(point.getCourseId());
        question.setKnowledgePointId(point.getId());
        question.setType(type);
        question.setContent(request.content().trim());
        question.setOptionsJson(writeOptions(options));
        question.setAnswer(answer);
        question.setAnalysis(request.analysis());
        if (id == null) {
            questionMapper.insert(question);
        } else {
            questionMapper.updateById(question);
        }
    }

    public void delete(Long id) {
        answerRecordMapper.delete(new LambdaQueryWrapper<AnswerRecord>().eq(AnswerRecord::getQuestionId, id));
        wrongQuestionMapper.delete(new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getQuestionId, id));
        questionMapper.deleteById(id);
    }

    private QuestionAdmin toAdmin(Question question) {
        Course course = question.getCourseId() == null ? null : courseMapper.selectById(question.getCourseId());
        KnowledgePoint point = question.getKnowledgePointId() == null ? null : pointMapper.selectById(question.getKnowledgePointId());
        return new QuestionAdmin(question.getId(), question.getCourseId(), course == null ? "" : course.getName(),
                question.getKnowledgePointId(), point == null ? "" : point.getName(), question.getType(), question.getContent(),
                readOptions(question.getOptionsJson()), question.getAnswer(), question.getAnalysis());
    }

    private List<String> readOptions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private String writeOptions(List<String> options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            throw BizException.bad("选项保存失败");
        }
    }
}
