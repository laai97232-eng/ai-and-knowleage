package com.study.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("study_plan_item")
public class StudyPlanItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private Integer dayIndex;
    private Long knowledgePointId;
    private String content;
    private String status;
    private String note;
}
