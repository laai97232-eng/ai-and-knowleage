package com.study.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.assistant.common.BizException;
import com.study.assistant.entity.AnswerRecord;
import com.study.assistant.entity.Chapter;
import com.study.assistant.entity.Course;
import com.study.assistant.entity.KnowledgePoint;
import com.study.assistant.entity.KnowledgeRelation;
import com.study.assistant.entity.LearningDocument;
import com.study.assistant.entity.LearningProgress;
import com.study.assistant.entity.Question;
import com.study.assistant.entity.StudyPlanItem;
import com.study.assistant.entity.StudyRecord;
import com.study.assistant.entity.WrongQuestion;
import com.study.assistant.mapper.AnswerRecordMapper;
import com.study.assistant.mapper.ChapterMapper;
import com.study.assistant.mapper.CourseMapper;
import com.study.assistant.mapper.KnowledgePointMapper;
import com.study.assistant.mapper.KnowledgeRelationMapper;
import com.study.assistant.mapper.LearningDocumentMapper;
import com.study.assistant.mapper.LearningProgressMapper;
import com.study.assistant.mapper.QuestionMapper;
import com.study.assistant.mapper.StudyPlanItemMapper;
import com.study.assistant.mapper.StudyRecordMapper;
import com.study.assistant.mapper.WrongQuestionMapper;
import com.study.assistant.model.ApiModels.ChapterNode;
import com.study.assistant.model.ApiModels.ChapterSave;
import com.study.assistant.model.ApiModels.CourseCard;
import com.study.assistant.model.ApiModels.CourseDetail;
import com.study.assistant.model.ApiModels.CourseSave;
import com.study.assistant.model.ApiModels.DocBrief;
import com.study.assistant.model.ApiModels.KnowledgeDetail;
import com.study.assistant.model.ApiModels.KnowledgeOption;
import com.study.assistant.model.ApiModels.KnowledgeSave;
import com.study.assistant.model.ApiModels.NameRef;
import com.study.assistant.model.ApiModels.PointBrief;
import com.study.assistant.model.ApiModels.QuestionBrief;
import com.study.assistant.model.ApiModels.RelationSave;
import com.study.assistant.model.ApiModels.RelationView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CourseService {
    private final CourseMapper courseMapper;
    private final ChapterMapper chapterMapper;
    private final KnowledgePointMapper pointMapper;
    private final KnowledgeRelationMapper relationMapper;
    private final LearningDocumentMapper documentMapper;
    private final QuestionMapper questionMapper;
    private final LearningProgressMapper progressMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final StudyPlanItemMapper planItemMapper;
    private final StudyRecordMapper studyRecordMapper;
    private final DocumentService documentService;
    private final ProgressService progressService;
    private final IndexService indexService;

    public CourseService(CourseMapper courseMapper, ChapterMapper chapterMapper, KnowledgePointMapper pointMapper,
                         KnowledgeRelationMapper relationMapper, LearningDocumentMapper documentMapper,
                         QuestionMapper questionMapper, LearningProgressMapper progressMapper,
                         AnswerRecordMapper answerRecordMapper, WrongQuestionMapper wrongQuestionMapper,
                         StudyPlanItemMapper planItemMapper, StudyRecordMapper studyRecordMapper,
                         DocumentService documentService, ProgressService progressService, IndexService indexService) {
        this.courseMapper = courseMapper;
        this.chapterMapper = chapterMapper;
        this.pointMapper = pointMapper;
        this.relationMapper = relationMapper;
        this.documentMapper = documentMapper;
        this.questionMapper = questionMapper;
        this.progressMapper = progressMapper;
        this.answerRecordMapper = answerRecordMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.planItemMapper = planItemMapper;
        this.studyRecordMapper = studyRecordMapper;
        this.documentService = documentService;
        this.progressService = progressService;
        this.indexService = indexService;
    }

    public List<CourseCard> listForStudent(Long userId, String keyword) {
        LambdaQueryWrapper<Course> query = new LambdaQueryWrapper<Course>().eq(Course::getStatus, 1).orderByAsc(Course::getId);
        if (keyword != null && !keyword.isBlank()) {
            query.like(Course::getName, keyword.trim());
        }
        return courseMapper.selectList(query).stream().map(course -> toCard(course, userId)).toList();
    }

    public List<CourseCard> listForAdmin() {
        return courseMapper.selectList(new LambdaQueryWrapper<Course>().orderByAsc(Course::getId))
                .stream().map(course -> toCard(course, null)).toList();
    }

    public CourseDetail detail(Long courseId, Long userId, boolean admin) {
        Course course = requireCourse(courseId);
        if (!admin && (course.getStatus() == null || course.getStatus() != 1)) {
            throw BizException.bad("课程不存在");
        }
        return buildDetail(course, userId);
    }

    public KnowledgeDetail knowledgeDetail(Long pointId, Long userId) {
        KnowledgePoint point = requirePoint(pointId);
        Course course = requireCourse(point.getCourseId());
        Chapter chapter = chapterMapper.selectById(point.getChapterId());
        List<KnowledgeRelation> relations = relationMapper.selectList(new LambdaQueryWrapper<KnowledgeRelation>()
                .eq(KnowledgeRelation::getSourceId, pointId)
                .or()
                .eq(KnowledgeRelation::getTargetId, pointId));
        List<NameRef> prerequisites = new ArrayList<>();
        List<NameRef> nextPoints = new ArrayList<>();
        List<NameRef> related = new ArrayList<>();
        for (KnowledgeRelation relation : relations) {
            if ("PREREQUISITE".equals(relation.getRelationType()) && pointId.equals(relation.getTargetId())) {
                prerequisites.add(nameOf(relation.getSourceId()));
            } else if ("PREREQUISITE".equals(relation.getRelationType()) && pointId.equals(relation.getSourceId())) {
                nextPoints.add(nameOf(relation.getTargetId()));
            } else if ("RELATED".equals(relation.getRelationType())) {
                Long other = pointId.equals(relation.getSourceId()) ? relation.getTargetId() : relation.getSourceId();
                related.add(nameOf(other));
            }
        }
        List<DocBrief> documents = documentMapper.selectList(new LambdaQueryWrapper<LearningDocument>()
                        .eq(LearningDocument::getKnowledgePointId, pointId)
                        .eq(LearningDocument::getStatus, 1)
                        .orderByDesc(LearningDocument::getId))
                .stream()
                .map(doc -> new DocBrief(doc.getId(), doc.getTitle(), doc.getFileType(), doc.getCategory(), doc.getFileName()))
                .toList();
        List<QuestionBrief> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                        .eq(Question::getKnowledgePointId, pointId)
                        .orderByAsc(Question::getId))
                .stream()
                .map(q -> new QuestionBrief(q.getId(), q.getType(), q.getContent()))
                .toList();
        LearningProgress progress = progressService.find(userId, pointId);
        int value = progress == null || progress.getProgress() == null ? 0 : progress.getProgress();
        String status = progress == null || progress.getStatus() == null ? "NOT_STARTED" : progress.getStatus();
        return new KnowledgeDetail(point.getId(), point.getName(), point.getDescription(), course.getId(), course.getName(),
                chapter == null ? "" : chapter.getTitle(), prerequisites, nextPoints, related, documents, questions,
                value, status, progressService.masteryRate(userId, pointId));
    }

    @Transactional
    public Course createCourse(CourseSave request) {
        Course course = new Course();
        course.setName(request.name().trim());
        course.setDescription(request.description());
        course.setStatus(request.status() == null ? 1 : request.status());
        courseMapper.insert(course);
        return course;
    }

    @Transactional
    public void updateCourse(Long id, CourseSave request) {
        Course course = requireCourse(id);
        course.setName(request.name().trim());
        course.setDescription(request.description());
        if (request.status() != null) {
            course.setStatus(request.status());
        }
        courseMapper.updateById(course);
    }

    @Transactional
    public void deleteCourse(Long courseId) {
        requireCourse(courseId);
        List<Long> pointIds = pointMapper.selectList(new LambdaQueryWrapper<KnowledgePoint>().eq(KnowledgePoint::getCourseId, courseId))
                .stream().map(KnowledgePoint::getId).toList();
        if (!pointIds.isEmpty()) {
            List<Long> questionIds = questionMapper.selectList(new LambdaQueryWrapper<Question>().in(Question::getKnowledgePointId, pointIds))
                    .stream().map(Question::getId).toList();
            if (!questionIds.isEmpty()) {
                answerRecordMapper.delete(new LambdaQueryWrapper<AnswerRecord>().in(AnswerRecord::getQuestionId, questionIds));
                wrongQuestionMapper.delete(new LambdaQueryWrapper<WrongQuestion>().in(WrongQuestion::getQuestionId, questionIds));
                questionMapper.delete(new LambdaQueryWrapper<Question>().in(Question::getId, questionIds));
            }
            relationMapper.delete(new LambdaQueryWrapper<KnowledgeRelation>()
                    .in(KnowledgeRelation::getSourceId, pointIds)
                    .or()
                    .in(KnowledgeRelation::getTargetId, pointIds));
            progressMapper.delete(new LambdaQueryWrapper<LearningProgress>().in(LearningProgress::getKnowledgePointId, pointIds));
            planItemMapper.delete(new LambdaQueryWrapper<StudyPlanItem>().in(StudyPlanItem::getKnowledgePointId, pointIds));
            pointMapper.delete(new LambdaQueryWrapper<KnowledgePoint>().in(KnowledgePoint::getId, pointIds));
        }
        List<LearningDocument> documents = documentMapper.selectList(new LambdaQueryWrapper<LearningDocument>().eq(LearningDocument::getCourseId, courseId));
        for (LearningDocument document : documents) {
            documentService.deleteFile(document.getFilePath());
        }
        documentMapper.delete(new LambdaQueryWrapper<LearningDocument>().eq(LearningDocument::getCourseId, courseId));
        studyRecordMapper.delete(new LambdaQueryWrapper<StudyRecord>().eq(StudyRecord::getCourseId, courseId));
        chapterMapper.delete(new LambdaQueryWrapper<Chapter>().eq(Chapter::getCourseId, courseId));
        indexService.removeCourse(courseId);
        courseMapper.deleteById(courseId);
    }

    @Transactional
    public Chapter createChapter(Long courseId, ChapterSave request) {
        requireCourse(courseId);
        Integer max = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>().eq(Chapter::getCourseId, courseId))
                .stream().map(Chapter::getSortOrder).filter(Objects::nonNull).max(Integer::compareTo).orElse(0);
        Chapter chapter = new Chapter();
        chapter.setCourseId(courseId);
        chapter.setTitle(request.title().trim());
        chapter.setSortOrder(max + 1);
        chapterMapper.insert(chapter);
        return chapter;
    }

    @Transactional
    public void updateChapter(Long id, ChapterSave request) {
        Chapter chapter = chapterMapper.selectById(id);
        if (chapter == null) {
            throw BizException.bad("章节不存在");
        }
        chapter.setTitle(request.title().trim());
        chapterMapper.updateById(chapter);
    }

    @Transactional
    public void deleteChapter(Long id) {
        Chapter chapter = chapterMapper.selectById(id);
        if (chapter == null) {
            throw BizException.bad("章节不存在");
        }
        Long count = pointMapper.selectCount(new LambdaQueryWrapper<KnowledgePoint>().eq(KnowledgePoint::getChapterId, id));
        if (count != null && count > 0) {
            throw BizException.bad("请先删除该章节下的知识点");
        }
        chapterMapper.deleteById(id);
    }

    @Transactional
    public KnowledgePoint createKnowledge(Long chapterId, KnowledgeSave request) {
        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw BizException.bad("章节不存在");
        }
        Integer max = pointMapper.selectList(new LambdaQueryWrapper<KnowledgePoint>().eq(KnowledgePoint::getChapterId, chapterId))
                .stream().map(KnowledgePoint::getSortOrder).filter(Objects::nonNull).max(Integer::compareTo).orElse(0);
        KnowledgePoint point = new KnowledgePoint();
        point.setCourseId(chapter.getCourseId());
        point.setChapterId(chapterId);
        point.setName(request.name().trim());
        point.setDescription(request.description());
        point.setSortOrder(max + 1);
        pointMapper.insert(point);
        indexService.indexKnowledge(point);
        return point;
    }

    @Transactional
    public void updateKnowledge(Long id, KnowledgeSave request) {
        KnowledgePoint point = requirePoint(id);
        point.setName(request.name().trim());
        point.setDescription(request.description());
        pointMapper.updateById(point);
        indexService.indexKnowledge(point);
    }

    @Transactional
    public void deleteKnowledge(Long id) {
        requirePoint(id);
        List<Long> questionIds = questionMapper.selectList(new LambdaQueryWrapper<Question>().eq(Question::getKnowledgePointId, id))
                .stream().map(Question::getId).toList();
        if (!questionIds.isEmpty()) {
            answerRecordMapper.delete(new LambdaQueryWrapper<AnswerRecord>().in(AnswerRecord::getQuestionId, questionIds));
            wrongQuestionMapper.delete(new LambdaQueryWrapper<WrongQuestion>().in(WrongQuestion::getQuestionId, questionIds));
            questionMapper.delete(new LambdaQueryWrapper<Question>().in(Question::getId, questionIds));
        }
        List<LearningDocument> documents = documentMapper.selectList(new LambdaQueryWrapper<LearningDocument>().eq(LearningDocument::getKnowledgePointId, id));
        for (LearningDocument document : documents) {
            documentService.deleteFile(document.getFilePath());
        }
        documentMapper.delete(new LambdaQueryWrapper<LearningDocument>().eq(LearningDocument::getKnowledgePointId, id));
        relationMapper.delete(new LambdaQueryWrapper<KnowledgeRelation>()
                .eq(KnowledgeRelation::getSourceId, id)
                .or()
                .eq(KnowledgeRelation::getTargetId, id));
        progressMapper.delete(new LambdaQueryWrapper<LearningProgress>().eq(LearningProgress::getKnowledgePointId, id));
        planItemMapper.delete(new LambdaQueryWrapper<StudyPlanItem>().eq(StudyPlanItem::getKnowledgePointId, id));
        studyRecordMapper.delete(new LambdaQueryWrapper<StudyRecord>().eq(StudyRecord::getKnowledgePointId, id));
        indexService.removeKnowledge(id);
        pointMapper.deleteById(id);
    }

    public List<KnowledgeOption> knowledgeOptions() {
        Map<Long, Course> courses = new HashMap<>();
        Map<Long, Chapter> chapters = new HashMap<>();
        courseMapper.selectList(null).forEach(course -> courses.put(course.getId(), course));
        chapterMapper.selectList(null).forEach(chapter -> chapters.put(chapter.getId(), chapter));
        return pointMapper.selectList(new LambdaQueryWrapper<KnowledgePoint>().orderByAsc(KnowledgePoint::getCourseId).orderByAsc(KnowledgePoint::getSortOrder))
                .stream()
                .map(point -> {
                    Course course = courses.get(point.getCourseId());
                    Chapter chapter = chapters.get(point.getChapterId());
                    return new KnowledgeOption(point.getId(), point.getName(), point.getCourseId(),
                            course == null ? "" : course.getName(), point.getChapterId(), chapter == null ? "" : chapter.getTitle());
                })
                .toList();
    }

    public List<RelationView> relations() {
        Map<Long, KnowledgePoint> points = new HashMap<>();
        pointMapper.selectList(null).forEach(point -> points.put(point.getId(), point));
        return relationMapper.selectList(new LambdaQueryWrapper<KnowledgeRelation>().orderByDesc(KnowledgeRelation::getId))
                .stream()
                .map(relation -> new RelationView(relation.getId(), relation.getSourceId(), name(points, relation.getSourceId()),
                        relation.getTargetId(), name(points, relation.getTargetId()), relation.getRelationType()))
                .toList();
    }

    @Transactional
    public void createRelation(RelationSave request) {
        if (request.sourceId().equals(request.targetId())) {
            throw BizException.bad("不能选择同一个知识点");
        }
        if (!"PREREQUISITE".equals(request.relationType()) && !"RELATED".equals(request.relationType())) {
            throw BizException.bad("关系类型不正确");
        }
        requirePoint(request.sourceId());
        requirePoint(request.targetId());
        Long exists = relationMapper.selectCount(new LambdaQueryWrapper<KnowledgeRelation>()
                .eq(KnowledgeRelation::getSourceId, request.sourceId())
                .eq(KnowledgeRelation::getTargetId, request.targetId())
                .eq(KnowledgeRelation::getRelationType, request.relationType()));
        if (exists != null && exists > 0) {
            throw BizException.bad("该关系已存在");
        }
        KnowledgeRelation relation = new KnowledgeRelation();
        relation.setSourceId(request.sourceId());
        relation.setTargetId(request.targetId());
        relation.setRelationType(request.relationType());
        relationMapper.insert(relation);
    }

    public void deleteRelation(Long id) {
        relationMapper.deleteById(id);
    }

    public KnowledgePoint requirePoint(Long id) {
        KnowledgePoint point = pointMapper.selectById(id);
        if (point == null) {
            throw BizException.bad("知识点不存在");
        }
        return point;
    }

    public Course requireCourse(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw BizException.bad("课程不存在");
        }
        return course;
    }

    private CourseDetail buildDetail(Course course, Long userId) {
        List<Chapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getCourseId, course.getId()).orderByAsc(Chapter::getSortOrder).orderByAsc(Chapter::getId));
        List<KnowledgePoint> points = pointMapper.selectList(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getCourseId, course.getId()).orderByAsc(KnowledgePoint::getSortOrder).orderByAsc(KnowledgePoint::getId));
        Map<Long, LearningProgress> progressMap = new HashMap<>();
        if (userId != null) {
            progressMapper.selectList(new LambdaQueryWrapper<LearningProgress>()
                            .eq(LearningProgress::getUserId, userId)
                            .eq(LearningProgress::getCourseId, course.getId()))
                    .forEach(item -> progressMap.put(item.getKnowledgePointId(), item));
        }
        List<ChapterNode> nodes = new ArrayList<>();
        int sum = 0;
        for (Chapter chapter : chapters) {
            List<PointBrief> briefs = new ArrayList<>();
            for (KnowledgePoint point : points) {
                if (!chapter.getId().equals(point.getChapterId())) {
                    continue;
                }
                LearningProgress progress = progressMap.get(point.getId());
                int value = progress == null || progress.getProgress() == null ? 0 : progress.getProgress();
                String status = progress == null || progress.getStatus() == null ? "NOT_STARTED" : progress.getStatus();
                int questionCount = Math.toIntExact(questionMapper.selectCount(new LambdaQueryWrapper<Question>().eq(Question::getKnowledgePointId, point.getId())));
                int documentCount = Math.toIntExact(documentMapper.selectCount(new LambdaQueryWrapper<LearningDocument>()
                        .eq(LearningDocument::getKnowledgePointId, point.getId())
                        .eq(LearningDocument::getStatus, 1)));
                briefs.add(new PointBrief(point.getId(), point.getName(), point.getDescription(), value, status, questionCount, documentCount));
                sum += value;
            }
            nodes.add(new ChapterNode(chapter.getId(), chapter.getTitle(), chapter.getSortOrder(), briefs));
        }
        int total = points.size();
        int progress = total == 0 ? 0 : sum / total;
        return new CourseDetail(course.getId(), course.getName(), course.getDescription(), course.getStatus(), progress, nodes);
    }

    private CourseCard toCard(Course course, Long userId) {
        List<KnowledgePoint> points = pointMapper.selectList(new LambdaQueryWrapper<KnowledgePoint>().eq(KnowledgePoint::getCourseId, course.getId()));
        int total = points.size();
        int sum = 0;
        int mastered = 0;
        if (userId != null && total > 0) {
            List<LearningProgress> progresses = progressMapper.selectList(new LambdaQueryWrapper<LearningProgress>()
                    .eq(LearningProgress::getUserId, userId)
                    .eq(LearningProgress::getCourseId, course.getId()));
            Map<Long, LearningProgress> map = new HashMap<>();
            progresses.forEach(item -> map.put(item.getKnowledgePointId(), item));
            for (KnowledgePoint point : points) {
                LearningProgress progress = map.get(point.getId());
                if (progress != null) {
                    sum += progress.getProgress() == null ? 0 : progress.getProgress();
                    if ("MASTERED".equals(progress.getStatus())) {
                        mastered++;
                    }
                }
            }
        }
        return new CourseCard(course.getId(), course.getName(), course.getDescription(), course.getStatus(),
                total == 0 ? 0 : sum / total, mastered, total);
    }

    private NameRef nameOf(Long id) {
        KnowledgePoint point = pointMapper.selectById(id);
        return new NameRef(id, point == null ? "已删除" : point.getName());
    }

    private String name(Map<Long, KnowledgePoint> points, Long id) {
        KnowledgePoint point = points.get(id);
        return point == null ? "已删除" : point.getName();
    }
}
