package com.david.hlp.web.resume.service;

import com.david.hlp.web.resume.entity.Resume;
import com.david.hlp.web.resume.entity.ResumeCommit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 简历版本控制服务
 * 实现Git风格的版本管理功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeVersionControlService {

    private final MongoTemplate mongoTemplate;

    /**
     * 初始化版本控制
     */
    public Resume initVersionControl(String resumeId, Long userId, String initialContent) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null) {
            throw new RuntimeException("简历不存在");
        }

        // 创建初始提交
        String initialCommitId = createCommit(resumeId, userId, "Initial commit",
                initialContent, "main", new ArrayList<>(), "normal");

        // 更新简历的版本控制信息
        resume.setHasVersionControl(true);
        resume.setCurrentBranch("main");
        resume.setHeadCommit(initialCommitId);
        resume.setRepositoryId(UUID.randomUUID().toString());
        resume.getBranches().put("main", initialCommitId);

        return mongoTemplate.save(resume);
    }

    /**
     * 创建提交
     */
    public String createCommit(String resumeId, Long userId, String message,
            String content, String branch, List<String> parentCommits, String commitType) {
        ResumeCommit commit = new ResumeCommit();
        commit.setId(generateCommitId());
        commit.setResumeId(resumeId);
        commit.setUserId(userId);
        commit.setMessage(message);
        commit.setContent(content);
        commit.setBranch(branch);
        commit.setParentCommits(parentCommits);
        commit.setCommitType(commitType);
        commit.setCommitTime(new Date());
        commit.setTreeHash(generateContentHash(content));

        mongoTemplate.save(commit);
        return commit.getId();
    }

    /**
     * 提交更改
     */
    public String commit(String resumeId, Long userId, String message, String content) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null || !resume.getHasVersionControl()) {
            throw new RuntimeException("简历不存在或未启用版本控制");
        }

        String currentBranch = resume.getCurrentBranch();
        List<String> parentCommits = new ArrayList<>();
        if (resume.getHeadCommit() != null) {
            parentCommits.add(resume.getHeadCommit());
        }

        String commitId = createCommit(resumeId, userId, message, content,
                currentBranch, parentCommits, "normal");

        // 更新简历的HEAD和分支指针
        resume.setHeadCommit(commitId);
        resume.getBranches().put(currentBranch, commitId);
        resume.setContent(content);

        mongoTemplate.save(resume);
        return commitId;
    }

    /**
     * 创建分支
     */
    public void createBranch(String resumeId, Long userId, String branchName) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null || !resume.getHasVersionControl()) {
            throw new RuntimeException("简历不存在或未启用版本控制");
        }

        if (resume.getBranches().containsKey(branchName)) {
            throw new RuntimeException("分支已存在: " + branchName);
        }

        // 从当前HEAD创建新分支
        resume.getBranches().put(branchName, resume.getHeadCommit());
        mongoTemplate.save(resume);
    }

    /**
     * 切换分支
     */
    public void switchBranch(String resumeId, Long userId, String branchName) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null || !resume.getHasVersionControl()) {
            throw new RuntimeException("简历不存在或未启用版本控制");
        }

        if (!resume.getBranches().containsKey(branchName)) {
            throw new RuntimeException("分支不存在: " + branchName);
        }

        String commitId = resume.getBranches().get(branchName);
        if (commitId == null) {
            throw new RuntimeException("分支 " + branchName + " 没有关联的提交");
        }

        ResumeCommit commit = getCommitById(commitId);
        if (commit == null) {
            throw new RuntimeException("提交 " + commitId + " 不存在");
        }

        resume.setCurrentBranch(branchName);
        resume.setHeadCommit(commitId);
        resume.setContent(commit.getContent());
        mongoTemplate.save(resume);
    }

    /**
     * 合并分支
     */
    public String mergeBranch(String resumeId, Long userId, String sourceBranch, String targetBranch) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null || !resume.getHasVersionControl()) {
            throw new RuntimeException("简历不存在或未启用版本控制");
        }

        String sourceCommitId = resume.getBranches().get(sourceBranch);
        String targetCommitId = resume.getBranches().get(targetBranch);

        if (sourceCommitId == null || targetCommitId == null) {
            throw new RuntimeException("源分支或目标分支不存在");
        }

        ResumeCommit sourceCommit = getCommitById(sourceCommitId);
        ResumeCommit targetCommit = getCommitById(targetCommitId);

        if (sourceCommit == null) {
            throw new RuntimeException("源分支的提交不存在: " + sourceCommitId);
        }
        if (targetCommit == null) {
            throw new RuntimeException("目标分支的提交不存在: " + targetCommitId);
        }

        // 简单合并策略：使用源分支的内容
        String mergeMessage = String.format("Merge branch '%s' into %s", sourceBranch, targetBranch);
        List<String> parentCommits = Arrays.asList(targetCommitId, sourceCommitId);

        String mergeCommitId = createCommit(resumeId, userId, mergeMessage,
                sourceCommit.getContent(), targetBranch, parentCommits, "merge");

        // 更新目标分支指针
        resume.getBranches().put(targetBranch, mergeCommitId);
        if (resume.getCurrentBranch().equals(targetBranch)) {
            resume.setHeadCommit(mergeCommitId);
            resume.setContent(sourceCommit.getContent());
        }

        mongoTemplate.save(resume);
        return mergeCommitId;
    }

    /**
     * 获取提交历史
     */
    public List<ResumeCommit> getCommitHistory(String resumeId, Long userId) {
        Query query = new Query(Criteria.where("resumeId").is(resumeId).and("userId").is(userId));
        return mongoTemplate.find(query, ResumeCommit.class);
    }

    /**
     * 检出到指定提交
     */
    public void checkout(String resumeId, Long userId, String commitId) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null || !resume.getHasVersionControl()) {
            throw new RuntimeException("简历不存在或未启用版本控制");
        }

        ResumeCommit commit = getCommitById(commitId);
        if (commit == null) {
            throw new RuntimeException("提交不存在: " + commitId);
        }

        resume.setHeadCommit(commitId);
        resume.setContent(commit.getContent());

        // 检查是否是某个分支的HEAD
        String branchName = resume.getBranches().entrySet().stream()
                .filter(entry -> entry.getValue().equals(commitId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (branchName != null) {
            resume.setCurrentBranch(branchName);
        } else {
            // 如果不是任何分支的HEAD，保持当前分支但处于分离HEAD状态
            log.warn("检出到分离HEAD状态: {}", commitId);
        }

        mongoTemplate.save(resume);
    }

    /**
     * 储藏当前更改
     */
    public void stash(String resumeId, Long userId, String content) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null || !resume.getHasVersionControl()) {
            throw new RuntimeException("简历不存在或未启用版本控制");
        }

        if (resume.getHeadCommit() == null) {
            throw new RuntimeException("当前分支没有提交历史，无法储藏");
        }

        Map<String, Object> stashEntry = new HashMap<>();
        stashEntry.put("message", String.format("WIP on %s: %s",
                resume.getCurrentBranch(),
                resume.getHeadCommit().substring(0, Math.min(7, resume.getHeadCommit().length()))));
        stashEntry.put("content", content);
        stashEntry.put("branch", resume.getCurrentBranch());
        stashEntry.put("head", resume.getHeadCommit());
        stashEntry.put("timestamp", new Date());

        resume.getStashStack().add(0, stashEntry); // 添加到栈顶
        mongoTemplate.save(resume);
    }

    /**
     * 弹出储藏
     */
    public String stashPop(String resumeId, Long userId) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null || resume.getStashStack().isEmpty()) {
            throw new RuntimeException("没有储藏条目");
        }

        Map<String, Object> stashEntry = resume.getStashStack().remove(0);
        String content = (String) stashEntry.get("content");

        resume.setContent(content);
        mongoTemplate.save(resume);

        return content;
    }

    // 辅助方法
    private Resume getResumeById(String resumeId, Long userId) {
        Query query = new Query(Criteria.where("id").is(resumeId).and("userId").is(userId));
        return mongoTemplate.findOne(query, Resume.class);
    }

    private ResumeCommit getCommitById(String commitId) {
        return mongoTemplate.findById(commitId, ResumeCommit.class);
    }

    private String generateCommitId() {
        return "commit-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String generateContentHash(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString().substring(0, 12);
        }
    }
}