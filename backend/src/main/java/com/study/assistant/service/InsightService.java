package com.study.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.assistant.common.BizException;
import com.study.assistant.entity.AnswerRecord;
import com.study.assistant.entity.Chapter;
import com.study.assistant.entity.Course;
import com.study.assistant.entity.KnowledgePoint;
import com.study.assistant.entity.KnowledgeRelation;
import com.study.assistant.entity.LearningProgress;
import com.study.assistant.entity.StudyRecord;
import com.study.assistant.entity.WrongQuestion;
import com.study.assistant.mapper.AnswerRecordMapper;
import com.study.assistant.mapper.ChapterMapper;
import com.study.assistant.mapper.CourseMapper;
import com.study.assistant.mapper.KnowledgePointMapper;
import com.study.assistant.mapper.KnowledgeRelationMapper;
import com.study.assistant.mapper.LearningProgressMapper;
import com.study.assistant.mapper.StudyRecordMapper;
import com.study.assistant.mapper.WrongQuestionMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class InsightService {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MM-dd");

    private final CourseMapper courseMapper;
    private final ChapterMapper chapterMapper;
    private final KnowledgePointMapper pointMapper;
    private final KnowledgeRelationMapper relationMapper;
    private final LearningProgressMapper progressMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final StudyRecordMapper studyRecordMapper;

    public InsightService(CourseMapper courseMapper, ChapterMapper chapterMapper, KnowledgePointMapper pointMapper,
                          KnowledgeRelationMapper relationMapper, LearningProgressMapper progressMapper,
                          AnswerRecordMapper answerRecordMapper, WrongQuestionMapper wrongQuestionMapper,
                          StudyRecordMapper studyRecordMapper) {
        this.courseMapper = courseMapper;
        this.chapterMapper = chapterMapper;
        this.pointMapper = pointMapper;
        this.relationMapper = relationMapper;
        this.progressMapper = progressMapper;
        this.answerRecordMapper = answerRecordMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.studyRecordMapper = studyRecordMapper;
    }

    public Map<String, Object> graph(Long userId, Long courseId) {
        Course course = requireCourse(courseId);
        List<KnowledgePoint> points = pointsOf(course.getId());
        Map<Long, Chapter> chapters = chaptersOf(course.getId());
        Map<Long, PointStat> stats = statsOf(userId, points);
        List<KnowledgeRelation> relations = relationsOf(points);
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (KnowledgePoint point : points) {
            PointStat stat = stats.get(point.getId());
            Chapter chapter = chapters.get(point.getChapterId());
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", point.getId());
            node.put("name", point.getName());
            node.put("chapter", chapter == null ? "" : chapter.getTitle());
            node.put("status", stat.status);
            node.put("mastery", stat.mastery);
            node.put("category", category(stat));
            nodes.add(node);
        }
        List<Map<String, Object>> edges = new ArrayList<>();
        for (KnowledgeRelation relation : relations) {
            Map<String, Object> edge = new LinkedHashMap<>();
            edge.put("source", relation.getSourceId());
            edge.put("target", relation.getTargetId());
            edge.put("type", relation.getRelationType());
            edges.add(edge);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("courseId", course.getId());
        result.put("courseName", course.getName());
        result.put("nodes", nodes);
        result.put("edges", edges);
        return result;
    }

    public Map<String, Object> insights(Long userId, Long courseId) {
        Course course = requireCourse(courseId);
        List<KnowledgePoint> points = pointsOf(course.getId());
        Map<Long, PointStat> stats = statsOf(userId, points);
        List<KnowledgeRelation> relations = relationsOf(points);
        List<Map<String, Object>> path = buildPath(points, relations, stats);
        List<Map<String, Object>> weak = weakPoints(points, stats);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("courseId", course.getId());
        result.put("courseName", course.getName());
        result.put("mastered", countStatus(stats, "MASTERED"));
        result.put("learning", countStatus(stats, "LEARNING"));
        result.put("notStarted", countStatus(stats, "NOT_STARTED"));
        result.put("weak", weak.size());
        result.put("attempts", stats.values().stream().mapToInt(stat -> stat.attempts).sum());
        result.put("accuracy", accuracy(stats));
        result.put("weakPoints", weak);
        result.put("path", path);
        result.put("recommendations", recommendations(points, relations, stats, path, weak));
        result.put("weekly", weekly(userId, course.getId()));
        return result;
    }

    private Course requireCourse(Long courseId) {
        Course course = courseId == null ? null : courseMapper.selectById(courseId);
        if (course == null || course.getStatus() == null || course.getStatus() != 1) {
            throw BizException.bad("课程不存在");
        }
        return course;
    }

    private List<KnowledgePoint> pointsOf(Long courseId) {
        return pointMapper.selectList(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getCourseId, courseId)
                .orderByAsc(KnowledgePoint::getSortOrder)
                .orderByAsc(KnowledgePoint::getId));
    }

    private Map<Long, Chapter> chaptersOf(Long courseId) {
        Map<Long, Chapter> map = new HashMap<>();
        for (Chapter chapter : chapterMapper.selectList(new LambdaQueryWrapper<Chapter>().eq(Chapter::getCourseId, courseId))) {
            map.put(chapter.getId(), chapter);
        }
        return map;
    }

    private List<KnowledgeRelation> relationsOf(List<KnowledgePoint> points) {
        Set<Long> ids = new HashSet<>();
        for (KnowledgePoint point : points) {
            ids.add(point.getId());
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        return relationMapper.selectList(new LambdaQueryWrapper<KnowledgeRelation>()
                        .in(KnowledgeRelation::getSourceId, ids)
                        .in(KnowledgeRelation::getTargetId, ids))
                .stream()
                .filter(relation -> ids.contains(relation.getSourceId()) && ids.contains(relation.getTargetId()))
                .toList();
    }

    private Map<Long, PointStat> statsOf(Long userId, List<KnowledgePoint> points) {
        Map<Long, PointStat> stats = new HashMap<>();
        if (points.isEmpty()) {
            return stats;
        }
        Set<Long> ids = new HashSet<>();
        for (KnowledgePoint point : points) {
            ids.add(point.getId());
            PointStat stat = new PointStat();
            stat.name = point.getName();
            stats.put(point.getId(), stat);
        }
        for (LearningProgress progress : progressMapper.selectList(new LambdaQueryWrapper<LearningProgress>()
                .eq(LearningProgress::getUserId, userId)
                .in(LearningProgress::getKnowledgePointId, ids))) {
            PointStat stat = stats.get(progress.getKnowledgePointId());
            if (stat != null && progress.getStatus() != null) {
                stat.status = progress.getStatus();
                stat.progress = progress.getProgress() == null ? 0 : progress.getProgress();
            }
        }
        for (AnswerRecord record : answerRecordMapper.selectList(new LambdaQueryWrapper<AnswerRecord>()
                .eq(AnswerRecord::getUserId, userId)
                .in(AnswerRecord::getKnowledgePointId, ids))) {
            PointStat stat = stats.get(record.getKnowledgePointId());
            if (stat == null) {
                continue;
            }
            stat.attempts++;
            if (record.getCorrect() != null && record.getCorrect() == 1) {
                stat.correct++;
            }
        }
        for (WrongQuestion wrong : wrongQuestionMapper.selectList(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .in(WrongQuestion::getKnowledgePointId, ids))) {
            PointStat stat = stats.get(wrong.getKnowledgePointId());
            if (stat != null) {
                stat.wrongCount += wrong.getWrongCount() == null ? 1 : wrong.getWrongCount();
            }
        }
        for (PointStat stat : stats.values()) {
            stat.mastery = stat.attempts == 0 ? stat.progress : (int) Math.round(stat.correct * 100.0 / stat.attempts);
            stat.weak = (stat.attempts > 0 && stat.mastery < 60) || (stat.wrongCount > 0 && stat.mastery < 80);
            if ("MASTERED".equals(stat.status) && stat.weak) {
                stat.status = "LEARNING";
            }
        }
        return stats;
    }

    private List<Map<String, Object>> weakPoints(List<KnowledgePoint> points, Map<Long, PointStat> stats) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (KnowledgePoint point : points) {
            PointStat stat = stats.get(point.getId());
            if (stat == null || !stat.weak) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", point.getId());
            row.put("name", point.getName());
            row.put("mastery", stat.mastery);
            row.put("attempts", stat.attempts);
            row.put("wrongCount", stat.wrongCount);
            row.put("reason", stat.wrongCount > 0
                    ? "错题 " + stat.wrongCount + " 次，正确率 " + stat.mastery + "%"
                    : "作答正确率 " + stat.mastery + "%，低于 60%");
            rows.add(row);
        }
        rows.sort(Comparator.comparingInt((Map<String, Object> row) -> (Integer) row.get("wrongCount")).reversed()
                .thenComparingInt(row -> (Integer) row.get("mastery")));
        return rows;
    }

    private List<Map<String, Object>> buildPath(List<KnowledgePoint> points, List<KnowledgeRelation> relations, Map<Long, PointStat> stats) {
        Map<Long, KnowledgePoint> byId = new HashMap<>();
        Map<Long, Integer> indegree = new HashMap<>();
        Map<Long, List<Long>> next = new HashMap<>();
        Map<Long, List<Long>> prereqs = new HashMap<>();
        for (KnowledgePoint point : points) {
            byId.put(point.getId(), point);
            indegree.put(point.getId(), 0);
            next.put(point.getId(), new ArrayList<>());
            prereqs.put(point.getId(), new ArrayList<>());
        }
        for (KnowledgeRelation relation : relations) {
            if (!"PREREQUISITE".equals(relation.getRelationType())) {
                continue;
            }
            if (!byId.containsKey(relation.getSourceId()) || !byId.containsKey(relation.getTargetId())) {
                continue;
            }
            next.get(relation.getSourceId()).add(relation.getTargetId());
            prereqs.get(relation.getTargetId()).add(relation.getSourceId());
            indegree.merge(relation.getTargetId(), 1, Integer::sum);
        }
        List<Long> order = new ArrayList<>();
        Set<Long> placed = new HashSet<>();
        while (placed.size() < points.size()) {
            KnowledgePoint nextPoint = points.stream()
                    .filter(point -> !placed.contains(point.getId()) && indegree.get(point.getId()) == 0)
                    .min(Comparator.comparingLong(KnowledgePoint::getId))
                    .orElse(null);
            if (nextPoint == null) {
                points.stream().filter(point -> !placed.contains(point.getId()))
                        .min(Comparator.comparingLong(KnowledgePoint::getId))
                        .ifPresent(point -> order.add(point.getId()));
                if (order.isEmpty() || placed.contains(order.get(order.size() - 1))) {
                    break;
                }
                placed.add(order.get(order.size() - 1));
                continue;
            }
            order.add(nextPoint.getId());
            placed.add(nextPoint.getId());
            for (Long target : next.get(nextPoint.getId())) {
                indegree.merge(target, -1, Integer::sum);
            }
        }
        List<Map<String, Object>> path = new ArrayList<>();
        for (Long id : order) {
            KnowledgePoint point = byId.get(id);
            PointStat stat = stats.get(id);
            String state = pathState(id, prereqs, stats);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", id);
            row.put("name", point.getName());
            row.put("state", state);
            row.put("mastery", stat == null ? 0 : stat.mastery);
            row.put("reason", pathReason(id, state, prereqs, byId, stats));
            path.add(row);
        }
        return path;
    }

    private String pathState(Long id, Map<Long, List<Long>> prereqs, Map<Long, PointStat> stats) {
        PointStat self = stats.get(id);
        if (self != null && "MASTERED".equals(self.status) && !self.weak) {
            return "DONE";
        }
        for (Long pre : prereqs.getOrDefault(id, List.of())) {
            PointStat stat = stats.get(pre);
            if (stat == null || !"MASTERED".equals(stat.status) || stat.weak) {
                return "BLOCKED";
            }
        }
        return "READY";
    }

    private String pathReason(Long id, String state, Map<Long, List<Long>> prereqs, Map<Long, KnowledgePoint> byId, Map<Long, PointStat> stats) {
        if ("DONE".equals(state)) {
            return "已经掌握";
        }
        if ("READY".equals(state)) {
            return prereqs.getOrDefault(id, List.of()).isEmpty() ? "没有前置知识，可以直接学" : "前置知识已经掌握";
        }
        List<String> names = new ArrayList<>();
        for (Long pre : prereqs.getOrDefault(id, List.of())) {
            PointStat stat = stats.get(pre);
            if (stat == null || !"MASTERED".equals(stat.status) || stat.weak) {
                KnowledgePoint point = byId.get(pre);
                if (point != null) {
                    names.add(point.getName());
                }
            }
        }
        return names.isEmpty() ? "还需要先补前置知识" : "先学习：" + String.join("、", names);
    }

    private List<Map<String, Object>> recommendations(List<KnowledgePoint> points, List<KnowledgeRelation> relations,
                                                      Map<Long, PointStat> stats, List<Map<String, Object>> path,
                                                      List<Map<String, Object>> weak) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        for (Map<String, Object> item : weak) {
            if (rows.size() >= 3) {
                break;
            }
            Long id = (Long) item.get("id");
            used.add(id);
            rows.add(advice(id, (String) item.get("name"), "REVIEW", "正确率偏低，建议先复习再做题"));
        }
        for (Map<String, Object> step : path) {
            if (rows.stream().filter(row -> "NEXT".equals(row.get("kind"))).count() >= 3) {
                break;
            }
            if (!"READY".equals(step.get("state"))) {
                continue;
            }
            Long id = (Long) step.get("id");
            if (used.contains(id)) {
                continue;
            }
            used.add(id);
            rows.add(advice(id, (String) step.get("name"), "NEXT", (String) step.get("reason")));
        }
        Map<Long, KnowledgePoint> byId = new HashMap<>();
        for (KnowledgePoint point : points) {
            byId.put(point.getId(), point);
        }
        for (KnowledgeRelation relation : relations) {
            if (rows.stream().filter(row -> "RELATED".equals(row.get("kind"))).count() >= 2) {
                break;
            }
            if (!"RELATED".equals(relation.getRelationType())) {
                continue;
            }
            PointStat source = stats.get(relation.getSourceId());
            PointStat target = stats.get(relation.getTargetId());
            if (source == null || target == null || "NOT_STARTED".equals(source.status) && !source.weak) {
                continue;
            }
            Long id = relation.getTargetId();
            if (used.contains(id) || "MASTERED".equals(target.status) && !target.weak) {
                continue;
            }
            KnowledgePoint point = byId.get(id);
            if (point == null) {
                continue;
            }
            used.add(id);
            rows.add(advice(id, point.getName(), "RELATED", "和正在学的「" + source.name + "」相关"));
        }
        return rows;
    }

    private Map<String, Object> advice(Long id, String name, String kind, String reason) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("name", name);
        row.put("kind", kind);
        row.put("reason", reason);
        return row;
    }

    private List<Map<String, Object>> weekly(Long userId, Long courseId) {
        LocalDate start = LocalDate.now().minusDays(6);
        Map<LocalDate, Integer> minutes = new HashMap<>();
        for (StudyRecord record : studyRecordMapper.selectList(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, userId)
                .eq(StudyRecord::getCourseId, courseId)
                .ge(StudyRecord::getStudyDate, start))) {
            if (record.getStudyDate() == null) {
                continue;
            }
            minutes.merge(record.getStudyDate(), record.getDurationMinutes() == null ? 0 : record.getDurationMinutes(), Integer::sum);
        }
        List<Map<String, Object>> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = start.plusDays(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", day.format(DAY));
            row.put("minutes", minutes.getOrDefault(day, 0));
            days.add(row);
        }
        return days;
    }

    private int countStatus(Map<Long, PointStat> stats, String status) {
        int count = 0;
        for (PointStat stat : stats.values()) {
            if (status.equals(stat.status) && !stat.weak) {
                count++;
            }
        }
        return count;
    }

    private int accuracy(Map<Long, PointStat> stats) {
        int attempts = 0;
        int correct = 0;
        for (PointStat stat : stats.values()) {
            attempts += stat.attempts;
            correct += stat.correct;
        }
        return attempts == 0 ? 0 : (int) Math.round(correct * 100.0 / attempts);
    }

    private int category(PointStat stat) {
        if (stat.weak) {
            return 2;
        }
        return switch (stat.status) {
            case "MASTERED" -> 0;
            case "LEARNING" -> 1;
            default -> 3;
        };
    }

    private static class PointStat {
        private String name = "";
        private String status = "NOT_STARTED";
        private int progress;
        private int attempts;
        private int correct;
        private int wrongCount;
        private int mastery;
        private boolean weak;
    }
}
