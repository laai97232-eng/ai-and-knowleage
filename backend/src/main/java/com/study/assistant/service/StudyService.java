package com.study.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.assistant.common.BizException;
import com.study.assistant.entity.Course;
import com.study.assistant.entity.KnowledgePoint;
import com.study.assistant.entity.StudyPlan;
import com.study.assistant.entity.StudyPlanItem;
import com.study.assistant.entity.StudyRecord;
import com.study.assistant.mapper.ChapterMapper;
import com.study.assistant.mapper.CourseMapper;
import com.study.assistant.mapper.KnowledgePointMapper;
import com.study.assistant.mapper.StudyPlanItemMapper;
import com.study.assistant.mapper.StudyPlanMapper;
import com.study.assistant.mapper.StudyRecordMapper;
import com.study.assistant.model.ApiModels.GeneratePlanRequest;
import com.study.assistant.model.ApiModels.PageResult;
import com.study.assistant.model.ApiModels.PlanDetail;
import com.study.assistant.model.ApiModels.PlanItemView;
import com.study.assistant.model.ApiModels.PlanView;
import com.study.assistant.model.ApiModels.RecordSave;
import com.study.assistant.model.ApiModels.RecordView;
import com.study.assistant.model.ApiModels.UpdatePlanItemRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StudyService {
    private final StudyRecordMapper recordMapper;
    private final StudyPlanMapper planMapper;
    private final StudyPlanItemMapper itemMapper;
    private final CourseMapper courseMapper;
    private final ChapterMapper chapterMapper;
    private final KnowledgePointMapper pointMapper;
    private final ProgressService progressService;

    public StudyService(StudyRecordMapper recordMapper, StudyPlanMapper planMapper, StudyPlanItemMapper itemMapper,
                        CourseMapper courseMapper, ChapterMapper chapterMapper, KnowledgePointMapper pointMapper,
                        ProgressService progressService) {
        this.recordMapper = recordMapper;
        this.planMapper = planMapper;
        this.itemMapper = itemMapper;
        this.courseMapper = courseMapper;
        this.chapterMapper = chapterMapper;
        this.pointMapper = pointMapper;
        this.progressService = progressService;
    }

    public PageResult<RecordView> records(Long userId, long page, long size) {
        Page<StudyRecord> result = recordMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<StudyRecord>().eq(StudyRecord::getUserId, userId).orderByDesc(StudyRecord::getStudyDate).orderByDesc(StudyRecord::getId));
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(),
                result.getRecords().stream().map(this::toRecord).toList());
    }

    public List<RecordView> recent(Long userId, int limit) {
        return recordMapper.selectList(new LambdaQueryWrapper<StudyRecord>()
                        .eq(StudyRecord::getUserId, userId)
                        .orderByDesc(StudyRecord::getStudyDate)
                        .orderByDesc(StudyRecord::getId)
                        .last("limit " + limit))
                .stream().map(this::toRecord).toList();
    }

    @Transactional
    public void addRecord(Long userId, RecordSave request) {
        Long courseId = request.courseId();
        Long pointId = request.knowledgePointId();
        if (pointId != null) {
            KnowledgePoint point = pointMapper.selectById(pointId);
            if (point == null) {
                throw BizException.bad("知识点不存在");
            }
            courseId = point.getCourseId();
        } else if (courseId != null && courseMapper.selectById(courseId) == null) {
            throw BizException.bad("课程不存在");
        }
        StudyRecord record = new StudyRecord();
        record.setUserId(userId);
        record.setCourseId(courseId);
        record.setKnowledgePointId(pointId);
        record.setContent(request.content());
        record.setDurationMinutes(request.durationMinutes());
        record.setNote(request.note());
        record.setStudyDate(request.studyDate() == null ? LocalDate.now() : request.studyDate());
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);
        progressService.markStudied(userId, courseId, pointId, 40);
    }

    public int minutesBetween(Long userId, LocalDate start, LocalDate end) {
        QueryWrapper<StudyRecord> query = new QueryWrapper<>();
        query.select("IFNULL(SUM(duration_minutes),0) AS total")
                .eq("user_id", userId)
                .between("study_date", start, end);
        return (int) scalar(recordMapper.selectMaps(query));
    }

    public long totalMinutes() {
        QueryWrapper<StudyRecord> query = new QueryWrapper<>();
        query.select("IFNULL(SUM(duration_minutes),0) AS total");
        return scalar(recordMapper.selectMaps(query));
    }

    private long scalar(List<Map<String, Object>> maps) {
        if (maps == null || maps.isEmpty() || maps.get(0) == null) {
            return 0;
        }
        Object value = maps.get(0).get("total");
        if (value == null) {
            value = maps.get(0).get("TOTAL");
        }
        if (value == null && !maps.get(0).isEmpty()) {
            value = maps.get(0).values().iterator().next();
        }
        return value instanceof Number number ? number.longValue() : 0;
    }

    public List<PlanView> plans(Long userId) {
        return planMapper.selectList(new LambdaQueryWrapper<StudyPlan>().eq(StudyPlan::getUserId, userId).orderByDesc(StudyPlan::getId))
                .stream()
                .map(plan -> {
                    List<StudyPlanItem> items = itemMapper.selectList(new LambdaQueryWrapper<StudyPlanItem>().eq(StudyPlanItem::getPlanId, plan.getId()));
                    int done = (int) items.stream().filter(item -> "DONE".equals(item.getStatus())).count();
                    return new PlanView(plan.getId(), plan.getTitle(), plan.getDescription(), plan.getStartDate(), plan.getEndDate(),
                            plan.getStatus(), done, items.size());
                })
                .toList();
    }

    public PlanDetail planDetail(Long userId, Long id) {
        StudyPlan plan = requirePlan(userId, id);
        List<PlanItemView> items = itemMapper.selectList(new LambdaQueryWrapper<StudyPlanItem>()
                        .eq(StudyPlanItem::getPlanId, id)
                        .orderByAsc(StudyPlanItem::getDayIndex)
                        .orderByAsc(StudyPlanItem::getId))
                .stream()
                .map(item -> {
                    KnowledgePoint point = item.getKnowledgePointId() == null ? null : pointMapper.selectById(item.getKnowledgePointId());
                    Course course = point == null ? null : courseMapper.selectById(point.getCourseId());
                    return new PlanItemView(item.getId(), item.getDayIndex(), item.getKnowledgePointId(),
                            point == null ? "" : point.getName(), course == null ? "" : course.getName(),
                            item.getContent(), item.getStatus(), item.getNote());
                })
                .toList();
        return new PlanDetail(plan.getId(), plan.getTitle(), plan.getDescription(), plan.getStartDate(), plan.getEndDate(), plan.getStatus(), items);
    }

    @Transactional
    public PlanDetail generate(Long userId, GeneratePlanRequest request) {
        Course course = courseMapper.selectById(request.courseId());
        if (course == null) {
            throw BizException.bad("课程不存在");
        }
        List<KnowledgePoint> points = orderedPoints(course.getId());
        if (points.isEmpty()) {
            throw BizException.bad("该课程还没有知识点");
        }
        LocalDate start = request.startDate() == null ? LocalDate.now() : request.startDate();
        StudyPlan plan = new StudyPlan();
        plan.setUserId(userId);
        plan.setTitle(request.title().trim());
        plan.setDescription("按《" + course.getName() + "》的知识点顺序，分配到 " + request.days() + " 天");
        plan.setStartDate(start);
        plan.setEndDate(start.plusDays(request.days() - 1L));
        plan.setStatus("ONGOING");
        plan.setCreatedAt(LocalDateTime.now());
        planMapper.insert(plan);
        int days = request.days();
        int size = points.size();
        for (int i = 0; i < size; i++) {
            KnowledgePoint point = points.get(i);
            StudyPlanItem item = new StudyPlanItem();
            item.setPlanId(plan.getId());
            item.setDayIndex((i * days) / size + 1);
            item.setKnowledgePointId(point.getId());
            item.setContent(point.getName());
            item.setStatus("TODO");
            itemMapper.insert(item);
        }
        return planDetail(userId, plan.getId());
    }

    @Transactional
    public void updateItem(Long userId, Long itemId, UpdatePlanItemRequest request) {
        StudyPlanItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw BizException.bad("计划内容不存在");
        }
        StudyPlan plan = requirePlan(userId, item.getPlanId());
        if (request.note() != null) {
            item.setNote(request.note());
        }
        if (request.status() != null && !request.status().isBlank()) {
            String status = request.status().trim().toUpperCase();
            if (!"TODO".equals(status) && !"DONE".equals(status)) {
                throw BizException.bad("状态不正确");
            }
            item.setStatus(status);
        }
        itemMapper.updateById(item);
        if ("DONE".equals(item.getStatus())) {
            progressService.markStudied(userId, courseIdOf(item.getKnowledgePointId()), item.getKnowledgePointId(), 70);
        }
        if (request.durationMinutes() != null && request.durationMinutes() > 0) {
            RecordSave record = new RecordSave(courseIdOf(item.getKnowledgePointId()), item.getKnowledgePointId(),
                    item.getContent(), request.durationMinutes(), request.note(), LocalDate.now());
            addRecord(userId, record);
        }
        refreshPlanStatus(plan.getId());
    }

    @Transactional
    public void deletePlan(Long userId, Long id) {
        requirePlan(userId, id);
        itemMapper.delete(new LambdaQueryWrapper<StudyPlanItem>().eq(StudyPlanItem::getPlanId, id));
        planMapper.deleteById(id);
    }

    private void refreshPlanStatus(Long planId) {
        List<StudyPlanItem> items = itemMapper.selectList(new LambdaQueryWrapper<StudyPlanItem>().eq(StudyPlanItem::getPlanId, planId));
        boolean allDone = !items.isEmpty() && items.stream().allMatch(item -> "DONE".equals(item.getStatus()));
        StudyPlan plan = planMapper.selectById(planId);
        if (plan != null) {
            plan.setStatus(allDone ? "DONE" : "ONGOING");
            planMapper.updateById(plan);
        }
    }

    private List<KnowledgePoint> orderedPoints(Long courseId) {
        Map<Long, Integer> chapterOrder = chapterMapper.selectList(null).stream()
                .filter(chapter -> courseId.equals(chapter.getCourseId()))
                .collect(Collectors.toMap(chapter -> chapter.getId(), chapter -> chapter.getSortOrder() == null ? 0 : chapter.getSortOrder()));
        List<KnowledgePoint> points = new ArrayList<>(pointMapper.selectList(new LambdaQueryWrapper<KnowledgePoint>().eq(KnowledgePoint::getCourseId, courseId)));
        points.sort(Comparator.comparingInt((KnowledgePoint point) -> chapterOrder.getOrDefault(point.getChapterId(), 0))
                .thenComparingInt(point -> point.getSortOrder() == null ? 0 : point.getSortOrder()));
        return points;
    }

    private StudyPlan requirePlan(Long userId, Long id) {
        StudyPlan plan = planMapper.selectById(id);
        if (plan == null || !userId.equals(plan.getUserId())) {
            throw BizException.bad("学习计划不存在");
        }
        return plan;
    }

    private Long courseIdOf(Long pointId) {
        if (pointId == null) {
            return null;
        }
        KnowledgePoint point = pointMapper.selectById(pointId);
        return point == null ? null : point.getCourseId();
    }

    private RecordView toRecord(StudyRecord record) {
        Course course = record.getCourseId() == null ? null : courseMapper.selectById(record.getCourseId());
        KnowledgePoint point = record.getKnowledgePointId() == null ? null : pointMapper.selectById(record.getKnowledgePointId());
        return new RecordView(record.getId(), record.getCourseId(), course == null ? "" : course.getName(),
                record.getKnowledgePointId(), point == null ? "" : point.getName(), record.getContent(),
                record.getDurationMinutes(), record.getNote(), record.getStudyDate());
    }
}
