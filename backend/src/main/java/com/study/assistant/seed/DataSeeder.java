package com.study.assistant.seed;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.assistant.config.AppProperties;
import com.study.assistant.entity.Chapter;
import com.study.assistant.entity.Course;
import com.study.assistant.entity.KnowledgePoint;
import com.study.assistant.entity.KnowledgeRelation;
import com.study.assistant.entity.LearningDocument;
import com.study.assistant.entity.Question;
import com.study.assistant.entity.User;
import com.study.assistant.mapper.ChapterMapper;
import com.study.assistant.mapper.CourseMapper;
import com.study.assistant.mapper.KnowledgePointMapper;
import com.study.assistant.mapper.KnowledgeRelationMapper;
import com.study.assistant.mapper.LearningDocumentMapper;
import com.study.assistant.mapper.QuestionMapper;
import com.study.assistant.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Order(0)
public class DataSeeder implements ApplicationRunner {
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final ChapterMapper chapterMapper;
    private final KnowledgePointMapper pointMapper;
    private final KnowledgeRelationMapper relationMapper;
    private final LearningDocumentMapper documentMapper;
    private final QuestionMapper questionMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final AppProperties props;

    public DataSeeder(UserMapper userMapper, CourseMapper courseMapper, ChapterMapper chapterMapper,
                      KnowledgePointMapper pointMapper, KnowledgeRelationMapper relationMapper,
                      LearningDocumentMapper documentMapper, QuestionMapper questionMapper,
                      PasswordEncoder passwordEncoder, ObjectMapper objectMapper, AppProperties props) {
        this.userMapper = userMapper;
        this.courseMapper = courseMapper;
        this.chapterMapper = chapterMapper;
        this.pointMapper = pointMapper;
        this.relationMapper = relationMapper;
        this.documentMapper = documentMapper;
        this.questionMapper = questionMapper;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        ensureUser("admin", "admin@study.local", "ADMIN");
        ensureUser("student", "student@study.local", "STUDENT");
        if (courseMapper.selectCount(null) > 0) {
            return;
        }
        Catalog catalog = objectMapper.readValue(new ClassPathResource("seed/catalog.json").getInputStream(), Catalog.class);
        for (CourseSeed courseSeed : catalog.courses) {
            Course course = new Course();
            course.setName(courseSeed.name);
            course.setDescription(courseSeed.description);
            course.setStatus(1);
            courseMapper.insert(course);
            Map<String, Long> pointIds = new HashMap<>();
            int chapterOrder = 1;
            if (courseSeed.chapters == null) {
                continue;
            }
            for (ChapterSeed chapterSeed : courseSeed.chapters) {
                Chapter chapter = new Chapter();
                chapter.setCourseId(course.getId());
                chapter.setTitle(chapterSeed.title);
                chapter.setSortOrder(chapterOrder++);
                chapterMapper.insert(chapter);
                int pointOrder = 1;
                if (chapterSeed.points == null) {
                    continue;
                }
                for (PointSeed pointSeed : chapterSeed.points) {
                    KnowledgePoint point = new KnowledgePoint();
                    point.setCourseId(course.getId());
                    point.setChapterId(chapter.getId());
                    point.setName(pointSeed.name);
                    point.setDescription(pointSeed.description);
                    point.setSortOrder(pointOrder++);
                    pointMapper.insert(point);
                    pointIds.put(pointSeed.name, point.getId());
                    if (pointSeed.document != null && pointSeed.document.content != null) {
                        String fileName = pointSeed.document.fileName;
                        Path file = props.uploadRoot().resolve("materials").resolve(fileName);
                        Files.writeString(file, pointSeed.document.content, StandardCharsets.UTF_8);
                        LearningDocument document = new LearningDocument();
                        document.setCourseId(course.getId());
                        document.setKnowledgePointId(point.getId());
                        document.setTitle(pointSeed.document.title);
                        document.setFileName(fileName);
                        document.setFilePath("materials/" + fileName);
                        document.setFileType(extension(fileName));
                        document.setCategory(pointSeed.document.category);
                        document.setStatus(1);
                        documentMapper.insert(document);
                    }
                    if (pointSeed.questions == null) {
                        continue;
                    }
                    for (QuestionSeed questionSeed : pointSeed.questions) {
                        Question question = new Question();
                        question.setCourseId(course.getId());
                        question.setKnowledgePointId(point.getId());
                        question.setType(questionSeed.type);
                        question.setContent(questionSeed.content);
                        question.setOptionsJson(objectMapper.writeValueAsString(questionSeed.options));
                        question.setAnswer(questionSeed.answer);
                        question.setAnalysis(questionSeed.analysis);
                        questionMapper.insert(question);
                    }
                }
            }
            if (courseSeed.relations == null) {
                continue;
            }
            for (RelationSeed relationSeed : courseSeed.relations) {
                Long source = pointIds.get(relationSeed.source);
                Long target = pointIds.get(relationSeed.target);
                if (source == null || target == null) {
                    continue;
                }
                KnowledgeRelation relation = new KnowledgeRelation();
                relation.setSourceId(source);
                relation.setTargetId(target);
                relation.setRelationType(relationSeed.type);
                relationMapper.insert(relation);
            }
        }
        log.info("已写入示例课程、知识点和题库");
    }

    private void ensureUser(String username, String email, String role) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count != null && count > 0) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("123456"));
        user.setEmail(email);
        user.setRole(role);
        user.setStatus(1);
        userMapper.insert(user);
    }

    private String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 ? "txt" : fileName.substring(index + 1);
    }

    public static class Catalog {
        public List<CourseSeed> courses;
    }

    public static class CourseSeed {
        public String name;
        public String description;
        public List<ChapterSeed> chapters;
        public List<RelationSeed> relations;
    }

    public static class ChapterSeed {
        public String title;
        public List<PointSeed> points;
    }

    public static class PointSeed {
        public String name;
        public String description;
        public DocSeed document;
        public List<QuestionSeed> questions;
    }

    public static class DocSeed {
        public String title;
        public String category;
        public String fileName;
        public String content;
    }

    public static class QuestionSeed {
        public String type;
        public String content;
        public List<String> options;
        public String answer;
        public String analysis;
    }

    public static class RelationSeed {
        public String source;
        public String target;
        public String type;
    }
}
