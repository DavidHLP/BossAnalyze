package com.david.hlp.web.resume.service;

import com.david.hlp.commons.utils.RedisCacheHelper;
import com.david.hlp.web.resume.entity.ResumeVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import com.david.hlp.web.resume.entity.Resume;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 简历服务类，处理简历的CRUD操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final MongoTemplate mongoTemplate;
    private final RedisCacheHelper redisCacheHelper;
    private final ResumeVersionService resumeVersionService;

    public List<Resume> getResumesByUserId(Long userId) {
        Query query = new Query(Criteria.where("user_id").is(userId));
        return mongoTemplate.find(query, Resume.class);
    }

    public Resume getResumeById(String id, Long userId) {
        Resume resume = mongoTemplate.findById(id, Resume.class);
        if (resume == null || !Objects.equals(resume.getUserId(), userId)) {
            // Or throw a custom exception
            return null;
        }
        return resume;
    }

    public Resume createResume(Resume resume, Long userId) {
        resume.setUserId(userId);
        resume.setCreatedAt(new Date());
        resume.setUpdatedAt(new Date());
        Resume savedResume = mongoTemplate.save(resume);

        // 创建初始版本
        resumeVersionService.createVersion(savedResume, "创建简历", false);

        return savedResume;
    }

    public Resume updateResume(String id, Resume resumeDetails, Long userId) {
        Resume existingResume = getResumeById(id, userId);
        if (existingResume == null) {
            return null; // or throw exception
        }

        // 检查内容是否发生变化
        boolean contentChanged = !Objects.equals(existingResume.getTitle(), resumeDetails.getTitle()) ||
                !Objects.equals(existingResume.getContent(), resumeDetails.getContent());

        existingResume.setTitle(resumeDetails.getTitle());
        existingResume.setContent(resumeDetails.getContent());
        existingResume.setUpdatedAt(new Date());
        Resume savedResume = mongoTemplate.save(existingResume);

        // 如果内容发生变化，创建新版本
        if (contentChanged) {
            resumeVersionService.createVersion(savedResume, "更新简历", false);
        }

        return savedResume;
    }

    public void deleteResume(String id, Long userId) {
        Resume existingResume = getResumeById(id, userId);
        if (existingResume != null) {
            // 删除简历的所有版本历史
            resumeVersionService.deleteVersionHistory(id, userId);
            // 删除简历本身
            mongoTemplate.remove(existingResume);
        }
        // consider throwing exception if not found
    }
}
