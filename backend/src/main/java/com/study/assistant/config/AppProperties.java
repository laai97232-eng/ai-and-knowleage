package com.study.assistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String uploadDir = "uploads";
    private String jwtSecret;
    private int jwtExpireHours = 72;
    /** 允许跨域访问的页面地址，同源部署时用不到；前后端分离时通过 APP_ALLOWED_ORIGINS 追加 */
    private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
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
