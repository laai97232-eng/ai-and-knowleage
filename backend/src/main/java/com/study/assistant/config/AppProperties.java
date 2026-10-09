package com.study.assistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String uploadDir = "uploads";
    private String jwtSecret;
    private int jwtExpireHours = 72;
    private Ai ai = new Ai();

    @Data
    public static class Ai {
        private String baseUrl = "";
        private String apiKey = "";
        private String chatModel = "";

        public boolean modelEnabled() {
            return apiKey != null && !apiKey.isBlank() && baseUrl != null && !baseUrl.isBlank();
        }
    }

    public Path uploadRoot() {
        Path path = Paths.get(uploadDir == null ? "uploads" : uploadDir);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path);
        }
        return path.toAbsolutePath().normalize();
    }
}
