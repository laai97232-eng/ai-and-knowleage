package com.study.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("learning_document")
public class LearningDocument {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long knowledgePointId;
    private String title;
    private String fileName;
    private String filePath;
    private String fileType;
    private String category;
    private Integer status;
    private Long uploaderId;
    private LocalDateTime createdAt;
}
