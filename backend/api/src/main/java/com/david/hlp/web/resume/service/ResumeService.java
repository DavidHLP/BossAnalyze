package com.david.hlp.web.resume.service;

import lombok.RequiredArgsConstructor;

import com.david.hlp.web.resume.entity.Resume;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 简历服务类，处理简历的CRUD操作
 */
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final MongoTemplate mongoTemplate;
    private final ResumeVersionControlService versionControlService;

    public List<Resume> getResumesByUserId(Long userId) {
        Query query = new Query(Criteria.where("user_id").is(userId).and("docType").is("meta"));
        return mongoTemplate.find(query, Resume.class);
    }

    public Resume getResumeById(String id, Long userId) {
        Query query = new Query(Criteria.where("id").is(id).and("userId").is(userId).and("docType").is("meta"));
        return mongoTemplate.findOne(query, Resume.class);
    }

    public Resume createResume(Resume request, Long userId) {
        Resume resume = new Resume();
        resume.setId(new ObjectId().toHexString());
        resume.setTitle(request.getTitle());
        resume.setContent(request.getContent());
        resume.setDocType("meta");
        resume.setUserId(userId);
        resume.setCreatedAt(new Date());
        resume.setUpdatedAt(new Date());

        // 自动初始化版本控制
        if (resume.getContent() == null) {
            resume.setContent("# 新建简历\n\n在此处开始编辑...");
        }

        return versionControlService.initialize(resume);
    }

    public Resume updateResume(String id, Long userId, String title, String content, Long expectedRevision) {
        return versionControlService.updateResume(id, userId, title, content, expectedRevision);
    }

    public void deleteResume(String id, Long userId, Long expectedRevision) {
        versionControlService.deleteResume(id, userId, expectedRevision);
    }
}
