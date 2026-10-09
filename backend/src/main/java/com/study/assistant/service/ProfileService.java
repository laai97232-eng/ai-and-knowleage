package com.study.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.assistant.common.BizException;
import com.study.assistant.config.AppProperties;
import com.study.assistant.entity.AnswerRecord;
import com.study.assistant.entity.Course;
import com.study.assistant.entity.KnowledgePoint;
import com.study.assistant.entity.LearningProgress;
import com.study.assistant.entity.User;
import com.study.assistant.entity.WrongQuestion;
import com.study.assistant.mapper.AnswerRecordMapper;
import com.study.assistant.mapper.CourseMapper;
import com.study.assistant.mapper.KnowledgePointMapper;
import com.study.assistant.mapper.LearningProgressMapper;
import com.study.assistant.mapper.UserMapper;
import com.study.assistant.mapper.WrongQuestionMapper;
import com.study.assistant.model.ApiModels.CourseStat;
import com.study.assistant.model.ApiModels.MasteryStat;
import com.study.assistant.model.ApiModels.PasswordUpdate;
import com.study.assistant.model.ApiModels.ProfileUpdate;
import com.study.assistant.model.ApiModels.StudentStats;
import com.study.assistant.model.ApiModels.UserProfile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ProfileService {
    private static final Set<String> IMAGES = Set.of("png", "jpg", "jpeg", "webp", "gif");
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CourseMapper courseMapper;
    private final KnowledgePointMapper pointMapper;
    private final LearningProgressMapper progressMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final StudyService studyService;
    private final AppProperties props;

    public ProfileService(UserMapper userMapper, PasswordEncoder passwordEncoder, CourseMapper courseMapper,
                          KnowledgePointMapper pointMapper, LearningProgressMapper progressMapper,
                          AnswerRecordMapper answerRecordMapper, WrongQuestionMapper wrongQuestionMapper,
                          StudyService studyService, AppProperties props) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.courseMapper = courseMapper;
        this.pointMapper = pointMapper;
        this.progressMapper = progressMapper;
        this.answerRecordMapper = answerRecordMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.studyService = studyService;
        this.props = props;
    }

    public User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        return user;
    }

    public UserProfile update(Long userId, ProfileUpdate request) {
        User user = requireUser(userId);
        if (request.email() != null) {
            user.setEmail(request.email().isBlank() ? null : request.email().trim());
        }
        userMapper.updateById(user);
        return AuthService.toProfile(user);
    }

    public void changePassword(Long userId, PasswordUpdate request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw BizException.bad("原密码不正确");
        }
        if (request.newPassword().length() < 6) {
            throw BizException.bad("新密码至少 6 位");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
    }

    public String saveAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.bad("请选择图片");
        }
        String original = file.getOriginalFilename() == null ? "avatar.png" : file.getOriginalFilename();
        String ext = extension(original);
        if (!IMAGES.contains(ext)) {
            throw BizException.bad("头像仅支持 png、jpg、webp、gif");
        }
        User user = requireUser(userId);
        String stored = userId + "." + ext;
        Path target = props.uploadRoot().resolve("avatars").resolve(stored);
        try {
            Files.copy(file.getInputStream(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw BizException.bad("头像保存失败");
        }
        user.setAvatar("avatars/" + stored);
        userMapper.updateById(user);
        return user.getAvatar();
    }

    public Path avatarPath(Long userId) {
        User user = requireUser(userId);
        if (user.getAvatar() == null || user.getAvatar().isBlank()) {
            throw BizException.bad("还没有头像");
        }
        Path root = props.uploadRoot();
        Path file = root.resolve(user.getAvatar()).normalize();
        if (!file.startsWith(root) || Files.notExists(file)) {
            throw BizException.bad("头像文件不存在");
        }
        return file;
    }

    public StudentStats stats(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate monthStart = today.withDayOfMonth(1);
        List<LearningProgress> progresses = progressMapper.selectList(new LambdaQueryWrapper<LearningProgress>().eq(LearningProgress::getUserId, userId));
        Map<Long, List<LearningProgress>> byCourse = new HashMap<>();
        for (LearningProgress progress : progresses) {
            byCourse.computeIfAbsent(progress.getCourseId(), key -> new ArrayList<>()).add(progress);
        }
        List<CourseStat> courseStats = new ArrayList<>();
        for (Map.Entry<Long, List<LearningProgress>> entry : byCourse.entrySet()) {
            Course course = courseMapper.selectById(entry.getKey());
            if (course == null) {
                continue;
            }
            int total = Math.toIntExact(pointMapper.selectCount(new LambdaQueryWrapper<KnowledgePoint>().eq(KnowledgePoint::getCourseId, course.getId())));
            int sum = entry.getValue().stream().mapToInt(item -> item.getProgress() == null ? 0 : item.getProgress()).sum();
            int mastered = (int) entry.getValue().stream().filter(item -> "MASTERED".equals(item.getStatus())).count();
            int progress = total == 0 ? 0 : sum / total;
            if (progress > 0 || mastered > 0) {
                courseStats.add(new CourseStat(course.getId(), course.getName(), progress, mastered, total));
            }
        }
        List<AnswerRecord> answers = answerRecordMapper.selectList(new LambdaQueryWrapper<AnswerRecord>().eq(AnswerRecord::getUserId, userId));
        Map<Long, List<AnswerRecord>> byPoint = new HashMap<>();
        for (AnswerRecord answer : answers) {
            if (answer.getKnowledgePointId() != null) {
                byPoint.computeIfAbsent(answer.getKnowledgePointId(), key -> new ArrayList<>()).add(answer);
            }
        }
        List<MasteryStat> mastery = new ArrayList<>();
        for (Map.Entry<Long, List<AnswerRecord>> entry : byPoint.entrySet()) {
            KnowledgePoint point = pointMapper.selectById(entry.getKey());
            if (point == null) {
                continue;
            }
            Course course = courseMapper.selectById(point.getCourseId());
            int correct = (int) entry.getValue().stream().filter(item -> item.getCorrect() != null && item.getCorrect() == 1).count();
            int rate = (int) Math.round(correct * 100.0 / entry.getValue().size());
            mastery.add(new MasteryStat(point.getId(), point.getName(), course == null ? "" : course.getName(), rate, entry.getValue().size()));
        }
        long wrongCount = wrongQuestionMapper.selectCount(new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getUserId, userId));
        return new StudentStats(
                studyService.minutesBetween(userId, today, today),
                studyService.minutesBetween(userId, weekStart, today),
                studyService.minutesBetween(userId, monthStart, today),
                wrongCount,
                courseStats, mastery, studyService.recent(userId, 5));
    }

    private String extension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
