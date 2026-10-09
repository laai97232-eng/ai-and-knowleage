package com.study.assistant;

import com.study.assistant.config.AppProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.study.assistant.mapper")
@EnableConfigurationProperties(AppProperties.class)
public class StudyAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyAssistantApplication.class, args);
    }
}
