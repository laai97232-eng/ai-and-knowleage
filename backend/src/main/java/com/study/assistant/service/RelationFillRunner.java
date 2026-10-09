package com.study.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.assistant.entity.KnowledgePoint;
import com.study.assistant.entity.KnowledgeRelation;
import com.study.assistant.mapper.KnowledgePointMapper;
import com.study.assistant.mapper.KnowledgeRelationMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(5)
public class RelationFillRunner implements ApplicationRunner {
    private final KnowledgePointMapper pointMapper;
    private final KnowledgeRelationMapper relationMapper;

    public RelationFillRunner(KnowledgePointMapper pointMapper, KnowledgeRelationMapper relationMapper) {
        this.pointMapper = pointMapper;
        this.relationMapper = relationMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        link("流程控制", "List", "PREREQUISITE");
        link("List", "Map", "PREREQUISITE");
        link("List", "Set", "RELATED");
        link("运算符", "流程控制", "RELATED");
    }

    private void link(String sourceName, String targetName, String type) {
        List<KnowledgePoint> sources = pointMapper.selectList(new LambdaQueryWrapper<KnowledgePoint>().eq(KnowledgePoint::getName, sourceName));
        List<KnowledgePoint> targets = pointMapper.selectList(new LambdaQueryWrapper<KnowledgePoint>().eq(KnowledgePoint::getName, targetName));
        for (KnowledgePoint source : sources) {
            for (KnowledgePoint target : targets) {
                if (source.getCourseId() == null || !source.getCourseId().equals(target.getCourseId())) {
                    continue;
                }
                Long exists = relationMapper.selectCount(new LambdaQueryWrapper<KnowledgeRelation>()
                        .eq(KnowledgeRelation::getSourceId, source.getId())
                        .eq(KnowledgeRelation::getTargetId, target.getId())
                        .eq(KnowledgeRelation::getRelationType, type));
                if (exists != null && exists > 0) {
                    continue;
                }
                KnowledgeRelation relation = new KnowledgeRelation();
                relation.setSourceId(source.getId());
                relation.setTargetId(target.getId());
                relation.setRelationType(type);
                relation.setCreatedAt(LocalDateTime.now());
                relationMapper.insert(relation);
            }
        }
    }
}
