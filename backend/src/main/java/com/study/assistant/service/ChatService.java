package com.study.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.assistant.common.BizException;
import com.study.assistant.config.AppProperties;
import com.study.assistant.entity.ChatMessage;
import com.study.assistant.entity.ChatSession;
import com.study.assistant.entity.Course;
import com.study.assistant.entity.DocumentChunk;
import com.study.assistant.mapper.ChatMessageMapper;
import com.study.assistant.mapper.ChatSessionMapper;
import com.study.assistant.mapper.CourseMapper;
import com.study.assistant.mapper.DocumentChunkMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {
    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final DocumentChunkMapper chunkMapper;
    private final CourseMapper courseMapper;
    private final EmbeddingService embeddingService;
    private final AppProperties props;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    public ChatService(ChatSessionMapper sessionMapper, ChatMessageMapper messageMapper, DocumentChunkMapper chunkMapper,
                       CourseMapper courseMapper, EmbeddingService embeddingService, AppProperties props, ObjectMapper objectMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.chunkMapper = chunkMapper;
        this.courseMapper = courseMapper;
        this.embeddingService = embeddingService;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> status() {
        Long count = chunkMapper.selectCount(null);
        return Map.of(
                "modelEnabled", props.getAi().modelEnabled(),
                "chunkCount", count == null ? 0 : count,
                "chatModel", props.getAi().getChatModel()
        );
    }

    public List<Map<String, Object>> sessions(Long userId) {
        return sessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .orderByDesc(ChatSession::getId))
                .stream()
                .map(session -> {
                    Course course = session.getCourseId() == null ? null : courseMapper.selectById(session.getCourseId());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", session.getId());
                    row.put("title", session.getTitle());
                    row.put("courseId", session.getCourseId());
                    row.put("courseName", course == null ? "" : course.getName());
                    row.put("createdAt", session.getCreatedAt());
                    return row;
                })
                .toList();
    }

    public Map<String, Object> detail(Long userId, Long sessionId) {
        ChatSession session = requireSession(userId, sessionId);
        List<Map<String, Object>> messages = messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getId))
                .stream()
                .map(message -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", message.getId());
                    row.put("role", message.getRole());
                    row.put("content", message.getContent());
                    row.put("sources", readSources(message.getSourcesJson()));
                    row.put("createdAt", message.getCreatedAt());
                    return row;
                })
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", session.getId());
        result.put("title", session.getTitle());
        result.put("courseId", session.getCourseId());
        result.put("messages", messages);
        return result;
    }

    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        requireSession(userId, sessionId);
        messageMapper.delete(new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getSessionId, sessionId));
        sessionMapper.deleteById(sessionId);
    }

    @Transactional
    public Map<String, Object> ask(Long userId, Long sessionId, Long courseId, String question) {
        String text = question == null ? "" : question.trim();
        if (text.length() < 2) {
            throw BizException.bad("请输入问题");
        }
        if (text.length() > 500) {
            throw BizException.bad("问题不要超过 500 字");
        }
        ChatSession session = sessionId == null ? createSession(userId, courseId, text) : requireSession(userId, sessionId);
        if (courseId != null) {
            session.setCourseId(courseId);
            sessionMapper.updateById(session);
        }
        List<Hit> hits = retrieve(text, session.getCourseId());
        boolean model = props.getAi().modelEnabled();
        String answer;
        String mode;
        try {
            if (model) {
                answer = askModel(session.getId(), text, hits);
                mode = "model";
            } else {
                answer = extractive(hits);
                mode = "extract";
            }
        } catch (Exception ex) {
            answer = extractive(hits);
            mode = "extract";
        }
        saveMessage(session.getId(), "user", text, List.of());
        List<Map<String, Object>> sources = hits.stream().map(Hit::source).toList();
        saveMessage(session.getId(), "assistant", answer, sources);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getId());
        result.put("answer", answer);
        result.put("sources", sources);
        result.put("mode", mode);
        return result;
    }

    private ChatSession createSession(Long userId, Long courseId, String question) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setCourseId(courseId);
        session.setTitle(question.length() > 24 ? question.substring(0, 24) + "…" : question);
        session.setCreatedAt(LocalDateTime.now());
        sessionMapper.insert(session);
        return session;
    }

    private ChatSession requireSession(Long userId, Long sessionId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw BizException.bad("对话不存在");
        }
        return session;
    }

    private List<Hit> retrieve(String question, Long courseId) {
        LambdaQueryWrapper<DocumentChunk> query = new LambdaQueryWrapper<>();
        if (courseId != null) {
            query.eq(DocumentChunk::getCourseId, courseId);
        }
        List<DocumentChunk> chunks = chunkMapper.selectList(query);
        float[] queryVector = embeddingService.embed(question);
        List<String> tokens = embeddingService.importantTokens(question);
        List<Hit> hits = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            float[] vector = readVector(chunk.getEmbeddingJson());
            float cosine = embeddingService.cosine(queryVector, vector);
            int matched = 0;
            String content = chunk.getContent() == null ? "" : chunk.getContent();
            for (String token : tokens) {
                if (content.contains(token)) {
                    matched++;
                }
            }
            float lexical = tokens.isEmpty() ? 0 : matched / (float) tokens.size();
            if (matched == 0 && cosine < 0.35f) {
                continue;
            }
            float score = lexical * 0.75f + cosine * 0.25f;
            hits.add(new Hit(chunk, score, content));
        }
        List<Hit> ranked = hits.stream()
                .sorted(Comparator.comparingDouble(Hit::score).reversed())
                .toList();
        if (ranked.isEmpty()) {
            return ranked;
        }
        float best = ranked.get(0).score();
        return ranked.stream().filter(hit -> hit.score() >= best * 0.62f).limit(3).toList();
    }

    private String extractive(List<Hit> hits) {
        if (hits.isEmpty()) {
            return "没有在已入库的课程资料里找到和这个问题直接相关的段落。可以换一种问法，或先选择对应课程后再问。";
        }
        StringBuilder text = new StringBuilder("根据课程资料，可以这样理解：\n\n");
        int index = 1;
        for (Hit hit : hits) {
            text.append(index++).append(". ").append(excerpt(hit.content(), 420)).append("\n\n");
        }
        text.append("以上内容来自课程资料，回答尽量贴合原文，没有把资料之外的内容补进去。");
        return text.toString().trim();
    }

    private String askModel(Long sessionId, String question, List<Hit> hits) {
        StringBuilder context = new StringBuilder();
        if (hits.isEmpty()) {
            context.append("没有检索到相关课程资料。");
        } else {
            int index = 1;
            for (Hit hit : hits) {
                context.append("[").append(index++).append("] ").append(hit.chunk().getTitle()).append("\n");
                context.append(excerpt(hit.content(), 700)).append("\n\n");
            }
        }
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "你是高校课程学习助手。只能根据给出的课程资料回答。回答使用中文，先解释问题，再指出依据来自哪一份资料。资料里没有的内容要直接说明没找到，不要编造。"));
        messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByDesc(ChatMessage::getId)
                        .last("limit 6"))
                .stream()
                .sorted(Comparator.comparing(ChatMessage::getId))
                .forEach(message -> messages.add(Map.of("role", message.getRole(), "content", message.getContent())));
        messages.add(Map.of("role", "user", "content", "课程资料：\n" + context + "\n学生问题：" + question));
        String url = props.getAi().getBaseUrl().replaceAll("/$", "") + "/chat/completions";
        Map<String, Object> body = Map.of(
                "model", props.getAi().getChatModel(),
                "temperature", 0.2,
                "messages", messages
        );
        String response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + props.getAi().getApiKey())
                .body(body)
                .retrieve()
                .body(String.class);
        try {
            JsonNode content = objectMapper.readTree(response).path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new IllegalStateException("empty");
            }
            return content.asText();
        } catch (Exception ex) {
            throw new IllegalStateException("模型返回无法解析");
        }
    }

    private void saveMessage(Long sessionId, String role, String content, List<Map<String, Object>> sources) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setSourcesJson(sources.isEmpty() ? null : writeJson(sources));
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
    }

    private String excerpt(String text, int limit) {
        String value = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        return value.length() <= limit ? value : value.substring(0, limit) + "…";
    }

    private float[] readVector(String json) {
        try {
            return objectMapper.readValue(json, float[].class);
        } catch (Exception ex) {
            return new float[EmbeddingService.DIM];
        }
    }

    private List<Map<String, Object>> readSources(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private record Hit(DocumentChunk chunk, float score, String content) {
        Map<String, Object> source() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("documentId", chunk.getDocumentId());
            row.put("knowledgePointId", chunk.getKnowledgePointId());
            row.put("title", chunk.getTitle());
            String brief = content.replaceAll("\\s+", " ").trim();
            row.put("excerpt", brief.length() <= 160 ? brief : brief.substring(0, 160) + "…");
            return row;
        }
    }
}
