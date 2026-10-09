package com.study.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.assistant.common.BizException;
import com.study.assistant.entity.User;
import com.study.assistant.mapper.UserMapper;
import com.study.assistant.model.ApiModels.LoginRequest;
import com.study.assistant.model.ApiModels.LoginResult;
import com.study.assistant.model.ApiModels.RegisterRequest;
import com.study.assistant.model.ApiModels.UserProfile;
import com.study.assistant.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResult login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.username().trim()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BizException.forbidden("账号已禁用");
        }
        return new LoginResult(jwtService.create(user), toProfile(user));
    }

    public UserProfile register(RegisterRequest request) {
        String username = request.username().trim();
        if (username.length() < 3 || username.length() > 20) {
            throw BizException.bad("用户名长度需要在 3 到 20 之间");
        }
        if (request.password().length() < 6) {
            throw BizException.bad("密码至少 6 位");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (exists != null && exists > 0) {
            throw BizException.bad("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(blankToNull(request.email()));
        user.setRole("STUDENT");
        user.setStatus(1);
        userMapper.insert(user);
        return toProfile(user);
    }

    public static UserProfile toProfile(User user) {
        return new UserProfile(user.getId(), user.getUsername(), user.getEmail(), user.getAvatar(), user.getRole(), user.getStatus());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
