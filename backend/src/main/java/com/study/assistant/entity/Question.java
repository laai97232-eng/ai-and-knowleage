package com.study.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long knowledgePointId;
    private String type;
    private String content;
    private String optionsJson;
    private String answer;
    private String analysis;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
