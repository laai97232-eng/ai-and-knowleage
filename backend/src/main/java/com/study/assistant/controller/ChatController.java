package com.study.assistant.controller;

import com.study.assistant.common.Auth;
import com.study.assistant.common.R;
import com.study.assistant.service.ChatService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/status")
    public R<Map<String, Object>> status() {
        Auth.require();
        return R.ok(chatService.status());
    }

    @GetMapping("/sessions")
    public R<?> sessions() {
        return R.ok(chatService.sessions(Auth.require().getId()));
    }

    @GetMapping("/sessions/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(chatService.detail(Auth.require().getId(), id));
    }

    @DeleteMapping("/sessions/{id}")
    public R<Void> delete(@PathVariable Long id) {
        chatService.deleteSession(Auth.require().getId(), id);
        return R.ok(null);
    }

    @PostMapping("/ask")
    public R<Map<String, Object>> ask(@RequestBody AskRequest request) {
        return R.ok(chatService.ask(Auth.require().getId(), request.sessionId(), request.courseId(), request.question()));
    }

    public record AskRequest(Long sessionId, Long courseId, String question) {
    }
}
