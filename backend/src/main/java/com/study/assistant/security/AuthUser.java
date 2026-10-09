package com.study.assistant.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthUser {
    private Long id;
    private String username;
    private String role;
}
