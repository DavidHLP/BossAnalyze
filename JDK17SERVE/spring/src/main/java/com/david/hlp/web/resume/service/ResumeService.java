package com.david.hlp.web.resume.service;

import com.david.hlp.commons.utils.RedisCacheHelper;
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
        return mongoTemplate.save(resume);
    }

    public Resume updateResume(String id, Resume resumeDetails, Long userId) {
        Resume existingResume = getResumeById(id, userId);
        if (existingResume == null) {
            return null; // or throw exception
        }
        existingResume.setTitle(resumeDetails.getTitle());
        existingResume.setContent(resumeDetails.getContent());
        existingResume.setUpdatedAt(new Date());
        return mongoTemplate.save(existingResume);
    }

    public void deleteResume(String id, Long userId) {
        Resume existingResume = getResumeById(id, userId);
        if (existingResume != null) {
            mongoTemplate.remove(existingResume);
        }
        // consider throwing exception if not found
    }
}
