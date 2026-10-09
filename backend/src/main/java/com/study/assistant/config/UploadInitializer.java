package com.study.assistant.config;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;

@Component
public class UploadInitializer {
    public UploadInitializer(AppProperties props) throws IOException {
        Files.createDirectories(props.uploadRoot().resolve("materials"));
        Files.createDirectories(props.uploadRoot().resolve("avatars"));
    }
}
