package com.david.hlp.web.resume.service;

import com.david.hlp.web.resume.entity.Resume;
import com.david.hlp.web.resume.entity.ResumeVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 简历版本管理服务类
 * 处理简历版本的创建、查询、恢复等操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeVersionService {

    private final MongoTemplate mongoTemplate;

    /**
     * 创建简历版本快照
     */
    public ResumeVersion createVersion(Resume resume, String changeDescription, boolean isAutoSave) {
        try {
            // 获取当前最大版本号
            Integer maxVersion = getMaxVersionNumber(resume.getId());

            ResumeVersion version = new ResumeVersion();
            version.setResumeId(resume.getId());
            version.setUserId(resume.getUserId());
            version.setVersionNumber(maxVersion + 1);
            version.setTitle(resume.getTitle());
            version.setContent(resume.getContent());
            version.setChangeDescription(changeDescription);
            version.setIsAutoSave(isAutoSave);
            version.setCreatedAt(new Date());

            ResumeVersion savedVersion = mongoTemplate.save(version);
            log.info("创建简历版本成功: resumeId={}, version={}", resume.getId(), savedVersion.getVersionNumber());
            return savedVersion;
        } catch (Exception e) {
            log.error("创建简历版本失败: resumeId={}", resume.getId(), e);
            throw new RuntimeException("创建版本失败", e);
        }
    }

    /**
     * 获取简历的所有版本历史
     */
    public List<ResumeVersion> getVersionHistory(String resumeId, Long userId) {
        Query query = new Query(Criteria.where("resume_id").is(resumeId)
                .and("user_id").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "version_number"));
        return mongoTemplate.find(query, ResumeVersion.class);
    }

    /**
     * 获取特定版本
     */
    public ResumeVersion getVersion(String resumeId, Integer versionNumber, Long userId) {
        Query query = new Query(Criteria.where("resume_id").is(resumeId)
                .and("version_number").is(versionNumber)
                .and("user_id").is(userId));
        return mongoTemplate.findOne(query, ResumeVersion.class);
    }

    /**
     * 恢复到指定版本
     */
    public Resume restoreToVersion(String resumeId, Integer versionNumber, Long userId) {
        // 获取要恢复的版本
        ResumeVersion targetVersion = getVersion(resumeId, versionNumber, userId);
        if (targetVersion == null) {
            throw new RuntimeException("指定版本不存在");
        }

        // 获取当前简历
        Resume currentResume = mongoTemplate.findById(resumeId, Resume.class);
        if (currentResume == null || !currentResume.getUserId().equals(userId)) {
            throw new RuntimeException("简历不存在或无权限");
        }

        // 先创建当前版本的快照
        createVersion(currentResume, "恢复版本前的自动备份", true);

        // 恢复到目标版本
        currentResume.setTitle(targetVersion.getTitle());
        currentResume.setContent(targetVersion.getContent());
        currentResume.setUpdatedAt(new Date());

        Resume restoredResume = mongoTemplate.save(currentResume);

        // 创建恢复操作的版本记录
        createVersion(restoredResume,
                String.format("恢复到版本 %d", versionNumber), false);

        log.info("恢复简历版本成功: resumeId={}, targetVersion={}", resumeId, versionNumber);
        return restoredResume;
    }

    /**
     * 删除简历的所有版本历史
     */
    public void deleteVersionHistory(String resumeId, Long userId) {
        Query query = new Query(Criteria.where("resume_id").is(resumeId)
                .and("user_id").is(userId));
        mongoTemplate.remove(query, ResumeVersion.class);
        log.info("删除简历版本历史: resumeId={}", resumeId);
    }

    /**
     * 获取最大版本号
     */
    private Integer getMaxVersionNumber(String resumeId) {
        Query query = new Query(Criteria.where("resume_id").is(resumeId))
                .with(Sort.by(Sort.Direction.DESC, "version_number"))
                .limit(1);
        ResumeVersion latestVersion = mongoTemplate.findOne(query, ResumeVersion.class);
        return latestVersion != null ? latestVersion.getVersionNumber() : 0;
    }

    /**
     * 清理旧版本（保留最近的指定数量）
     */
    public void cleanOldVersions(String resumeId, Long userId, int keepCount) {
        List<ResumeVersion> versions = getVersionHistory(resumeId, userId);
        if (versions.size() > keepCount) {
            List<ResumeVersion> toDelete = versions.subList(keepCount, versions.size());
            for (ResumeVersion version : toDelete) {
                mongoTemplate.remove(version);
            }
            log.info("清理旧版本: resumeId={}, 删除{}个版本", resumeId, toDelete.size());
        }
    }
}