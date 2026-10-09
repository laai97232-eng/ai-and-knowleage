package com.study.assistant.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ApiModels {
    private ApiModels() {
    }

    public record LoginRequest(
            @NotBlank(message = "请输入用户名") String username,
            @NotBlank(message = "请输入密码") String password) {
    }

    public record RegisterRequest(
            @NotBlank(message = "请输入用户名") String username,
            @NotBlank(message = "请输入密码") String password,
            String email) {
    }

    public record UserProfile(Long id, String username, String email, String avatar, String role, Integer status) {
    }

    public record LoginResult(String token, UserProfile user) {
    }

    public record ProfileUpdate(String email) {
    }

    public record PasswordUpdate(
            @NotBlank(message = "请输入原密码") String oldPassword,
            @NotBlank(message = "请输入新密码") String newPassword) {
    }

    public record PageResult<T>(long total, long page, long size, List<T> records) {
    }

    public record CourseCard(Long id, String name, String description, Integer status, int progress, int mastered, int totalPoints) {
    }

    public record PointBrief(Long id, String name, String description, int progress, String status, int questionCount, int documentCount) {
    }

    public record ChapterNode(Long id, String title, Integer sortOrder, List<PointBrief> points) {
    }

    public record CourseDetail(Long id, String name, String description, Integer status, int progress, List<ChapterNode> chapters) {
    }

    public record NameRef(Long id, String name) {
    }

    public record DocBrief(Long id, String title, String fileType, String category, String fileName) {
    }

    public record QuestionBrief(Long id, String type, String content) {
    }

    public record KnowledgeDetail(
            Long id, String name, String description, Long courseId, String courseName, String chapterTitle,
            List<NameRef> prerequisites, List<NameRef> nextPoints, List<NameRef> related,
            List<DocBrief> documents, List<QuestionBrief> questions,
            int progress, String status, int mastery) {
    }

    public record CourseSave(@NotBlank(message = "请输入课程名称") String name, String description, Integer status) {
    }

    public record ChapterSave(@NotBlank(message = "请输入章节名称") String title) {
    }

    public record KnowledgeSave(@NotBlank(message = "请输入知识点名称") String name, String description) {
    }

    public record KnowledgeOption(Long id, String name, Long courseId, String courseName, Long chapterId, String chapterTitle) {
    }

    public record RelationView(Long id, Long sourceId, String sourceName, Long targetId, String targetName, String relationType) {
    }

    public record RelationSave(@NotNull Long sourceId, @NotNull Long targetId, @NotBlank String relationType) {
    }

    public record QuestionView(Long id, String type, String content, List<String> options) {
    }

    public record SubmitRequest(@NotBlank(message = "请选择答案") String answer) {
    }

    public record SubmitResult(boolean correct, String answer, String analysis) {
    }

    public record QuestionAdmin(
            Long id, Long courseId, String courseName, Long knowledgePointId, String knowledgeName,
            String type, String content, List<String> options, String answer, String analysis) {
    }

    public record QuestionSave(
            @NotNull Long knowledgePointId,
            @NotBlank String type,
            @NotBlank(message = "请输入题干") String content,
            List<String> options,
            @NotBlank(message = "请设置答案") String answer,
            String analysis) {
    }

    public record WrongView(
            Long id, Long questionId, String content, Long knowledgePointId, String knowledgeName,
            Integer wrongCount, String lastAnswer, LocalDateTime lastWrongAt) {
    }

    public record DocumentView(
            Long id, String title, String fileName, String fileType, String category, Integer status,
            Long courseId, String courseName, Long knowledgePointId, String knowledgeName, LocalDateTime createdAt) {
    }

    public record DocumentUpdate(String title, String category, Integer status, Long knowledgePointId) {
    }

    public record RecordView(
            Long id, Long courseId, String courseName, Long knowledgePointId, String knowledgeName,
            String content, Integer durationMinutes, String note, LocalDate studyDate) {
    }

    public record RecordSave(
            Long courseId, Long knowledgePointId, String content,
            @NotNull @Min(value = 1, message = "学习时长至少 1 分钟") @Max(600) Integer durationMinutes,
            String note, LocalDate studyDate) {
    }

    public record PlanItemView(
            Long id, Integer dayIndex, Long knowledgePointId, String knowledgeName, String courseName,
            String content, String status, String note) {
    }

    public record PlanView(
            Long id, String title, String description, LocalDate startDate, LocalDate endDate, String status, int done, int total) {
    }

    public record PlanDetail(
            Long id, String title, String description, LocalDate startDate, LocalDate endDate, String status,
            List<PlanItemView> items) {
    }

    public record GeneratePlanRequest(
            @NotBlank(message = "请输入计划标题") String title,
            @NotNull Long courseId,
            @NotNull @Min(1) @Max(90) Integer days,
            LocalDate startDate) {
    }

    public record UpdatePlanItemRequest(String status, String note, Integer durationMinutes) {
    }

    public record CourseStat(Long courseId, String courseName, int progress, int mastered, int total) {
    }

    public record MasteryStat(Long knowledgePointId, String name, String courseName, int rate, int answered) {
    }

    public record StudentStats(
            int todayMinutes, int weekMinutes, int monthMinutes, long wrongCount,
            List<CourseStat> courses, List<MasteryStat> mastery, List<RecordView> recentRecords) {
    }

    public record AdminStats(
            long userCount, long courseCount, long knowledgeCount, long questionCount,
            long documentCount, long studyMinutes, long answerCount) {
    }

    public record UserAdmin(Long id, String username, String email, String role, Integer status, LocalDateTime createdAt) {
    }

    public record UserUpdate(String email, String role, Integer status) {
    }
}
