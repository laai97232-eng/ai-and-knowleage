package com.study.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.assistant.entity.AnswerRecord;
import com.study.assistant.entity.LearningProgress;
import com.study.assistant.mapper.AnswerRecordMapper;
import com.study.assistant.mapper.LearningProgressMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProgressService {
    private final LearningProgressMapper progressMapper;
    private final AnswerRecordMapper answerRecordMapper;

    public ProgressService(LearningProgressMapper progressMapper, AnswerRecordMapper answerRecordMapper) {
        this.progressMapper = progressMapper;
        this.answerRecordMapper = answerRecordMapper;
    }

    public void markStudied(Long userId, Long courseId, Long pointId, int minProgress) {
        if (userId == null || courseId == null || pointId == null) {
            return;
        }
        LearningProgress progress = find(userId, pointId);
        if (progress == null) {
            progress = new LearningProgress();
            progress.setUserId(userId);
            progress.setCourseId(courseId);
            progress.setKnowledgePointId(pointId);
            progress.setProgress(minProgress);
            progress.setStatus("LEARNING");
            progress.setUpdatedAt(LocalDateTime.now());
            progressMapper.insert(progress);
            return;
        }
        int current = progress.getProgress() == null ? 0 : progress.getProgress();
        progress.setProgress(Math.max(current, minProgress));
        if (!"MASTERED".equals(progress.getStatus())) {
            progress.setStatus(progress.getProgress() >= 100 ? "MASTERED" : "LEARNING");
        }
        if (progress.getProgress() >= 100) {
            progress.setProgress(100);
            progress.setStatus("MASTERED");
        }
        progress.setUpdatedAt(LocalDateTime.now());
        progressMapper.updateById(progress);
    }

    public void refreshByQuiz(Long userId, Long courseId, Long pointId) {
        if (pointId == null) {
            return;
        }
        List<AnswerRecord> records = answerRecordMapper.selectList(new LambdaQueryWrapper<AnswerRecord>()
                .eq(AnswerRecord::getUserId, userId)
                .eq(AnswerRecord::getKnowledgePointId, pointId));
        int total = records.size();
        int correct = (int) records.stream().filter(r -> r.getCorrect() != null && r.getCorrect() == 1).count();
        int rate = total == 0 ? 0 : (int) Math.round(correct * 100.0 / total);
        LearningProgress progress = find(userId, pointId);
        if (progress == null) {
            progress = new LearningProgress();
            progress.setUserId(userId);
            progress.setCourseId(courseId);
            progress.setKnowledgePointId(pointId);
            progress.setProgress(rate);
            progress.setStatus("LEARNING");
            progress.setUpdatedAt(LocalDateTime.now());
            if (rate >= 80 && total >= 2) {
                progress.setProgress(100);
                progress.setStatus("MASTERED");
            }
            progressMapper.insert(progress);
            return;
        }
        int current = progress.getProgress() == null ? 0 : progress.getProgress();
        if (rate >= 80 && total >= 2) {
            progress.setProgress(100);
            progress.setStatus("MASTERED");
        } else {
            progress.setProgress(Math.max(current, rate));
            if (!"MASTERED".equals(progress.getStatus())) {
                progress.setStatus("LEARNING");
            }
        }
        progress.setUpdatedAt(LocalDateTime.now());
        progressMapper.updateById(progress);
    }

    public int masteryRate(Long userId, Long pointId) {
        List<AnswerRecord> records = answerRecordMapper.selectList(new LambdaQueryWrapper<AnswerRecord>()
                .eq(AnswerRecord::getUserId, userId)
                .eq(AnswerRecord::getKnowledgePointId, pointId));
        if (records.isEmpty()) {
            return 0;
        }
        long correct = records.stream().filter(r -> r.getCorrect() != null && r.getCorrect() == 1).count();
        return (int) Math.round(correct * 100.0 / records.size());
    }

    public LearningProgress find(Long userId, Long pointId) {
        return progressMapper.selectOne(new LambdaQueryWrapper<LearningProgress>()
                .eq(LearningProgress::getUserId, userId)
                .eq(LearningProgress::getKnowledgePointId, pointId));
    }
}
