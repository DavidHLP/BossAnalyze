package com.david.hlp.web.resume.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import com.david.hlp.web.resume.entity.Resume;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * 简历服务类，处理简历的CRUD操作
 */
@Slf4j
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

    public Resume createResume(Resume resume, Long userId) {
        resume.setUserId(userId);
        resume.setCreatedAt(new Date());
        resume.setUpdatedAt(new Date());

        // 自动初始化版本控制
        String initialContent = resume.getContent() != null ? resume.getContent() : "# 新建简历\n\n在此处开始编辑...";
        if (resume.getContent() == null)
            resume.setContent(initialContent);

        // 先保存一次以获取ID
        mongoTemplate.save(resume);

        // 创建初始提交
        String initialCommitId = versionControlService.createCommit(
                resume.getId(),
                userId,
                "Initial commit",
                initialContent,
                "main",
                new ArrayList<>(),
                "normal");

        // 更新简历的版本控制元数据
        resume.setCurrentBranch("main");
        resume.setHeadCommit(initialCommitId);
        resume.setRepositoryId(UUID.randomUUID().toString());
        resume.getBranches().put("main", initialCommitId);

        return mongoTemplate.save(resume);
    }

    public Resume updateResume(String id, Resume resumeDetails, Long userId) {
        Resume existingResume = getResumeById(id, userId);
        if (existingResume == null) {
            return null; // or throw exception
        }
        String newContent = resumeDetails.getContent();
        existingResume.setUpdatedAt(new Date());

        String message = "Updated via basic save";
        versionControlService.commit(id, userId, message, newContent);

        return getResumeById(id, userId);
    }

    public void deleteResume(String id, Long userId) {
        Resume existingResume = getResumeById(id, userId);
        if (existingResume != null) {
            // 同时删除所有关联的commit
            Query commitsQuery = new Query(Criteria.where("resume_id").is(id).and("doc_type").is("commit"));
            mongoTemplate.remove(commitsQuery, Resume.class);

            // 删除简历元数据
            mongoTemplate.remove(existingResume);
        }
    }
}
