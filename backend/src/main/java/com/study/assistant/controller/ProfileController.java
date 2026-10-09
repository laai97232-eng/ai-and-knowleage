package com.study.assistant.controller;

import com.study.assistant.common.Auth;
import com.study.assistant.common.R;
import com.study.assistant.model.ApiModels.PasswordUpdate;
import com.study.assistant.model.ApiModels.ProfileUpdate;
import com.study.assistant.model.ApiModels.StudentStats;
import com.study.assistant.model.ApiModels.UserProfile;
import com.study.assistant.security.AuthUser;
import com.study.assistant.service.AuthService;
import com.study.assistant.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public R<UserProfile> me() {
        AuthUser auth = Auth.require();
        return R.ok(AuthService.toProfile(profileService.requireUser(auth.getId())));
    }

    @PutMapping
    public R<UserProfile> update(@RequestBody ProfileUpdate request) {
        return R.ok(profileService.update(Auth.require().getId(), request));
    }

    @PutMapping("/password")
    public R<Void> password(@Valid @RequestBody PasswordUpdate request) {
        profileService.changePassword(Auth.require().getId(), request);
        return R.ok(null);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<String> avatar(@RequestPart("file") MultipartFile file) {
        return R.ok(profileService.saveAvatar(Auth.require().getId(), file));
    }

    @GetMapping("/avatar")
    public org.springframework.http.ResponseEntity<byte[]> avatarFile() throws java.io.IOException {
        Path path = profileService.avatarPath(Auth.require().getId());
        String type = Files.probeContentType(path);
        return org.springframework.http.ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(type == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : type))
                .body(Files.readAllBytes(path));
    }

    @GetMapping("/stats")
    public R<StudentStats> stats() {
        return R.ok(profileService.stats(Auth.require().getId()));
    }
}
