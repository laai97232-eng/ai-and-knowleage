package com.study.assistant.controller;

import com.study.assistant.common.R;
import com.study.assistant.model.ApiModels.LoginRequest;
import com.study.assistant.model.ApiModels.LoginResult;
import com.study.assistant.model.ApiModels.RegisterRequest;
import com.study.assistant.model.ApiModels.UserProfile;
import com.study.assistant.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public R<LoginResult> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @PostMapping("/register")
    public R<UserProfile> register(@Valid @RequestBody RegisterRequest request) {
        return R.ok(authService.register(request));
    }
}
