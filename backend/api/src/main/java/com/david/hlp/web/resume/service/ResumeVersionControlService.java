package com.david.hlp.web.resume.service;

import com.david.hlp.web.resume.entity.Resume;
import lombok.RequiredArgsConstructor;
import com.david.hlp.web.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

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
     * 创建提交，这是版本控制的核心操作
     */
    public String createCommit(String resumeId, Long userId, String message,
            String content, String branch, List<String> parentCommits, String commitType) {
        if (getResumeById(resumeId, userId) == null) {
            throw new BusinessException(404, "简历不存在");
        }
        for (String parentId : parentCommits) {
            getCommitById(resumeId, userId, parentId);
        }
        Resume commit = new Resume();
        String commitId = generateCommitId();
        commit.setId(commitId);
        commit.setDocType("commit");
        log.info("🚀 创建提交 - ID: {}, 简历: {}, 用户: {}, 消息: {}", commitId, resumeId, userId, message);

        commit.setResumeId(resumeId);
        commit.setUserId(userId);
        commit.setMessage(message);
        commit.setContent(content);
        commit.setBranch(branch);
        commit.setParentCommits(parentCommits);
        commit.setCommitType(commitType);
        commit.setCommitTime(new Date());
        commit.setTreeHash(generateContentHash(content));

        mongoTemplate.insert(commit);
        log.info("✅ 提交保存成功 - ID: {}", commitId);
        return commit.getId();
    }

    /**
     * 提交更改
     */
    public String commit(String resumeId, Long userId, String message, String content) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null) {
            throw new RuntimeException("简历不存在");
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
        resume.setContent(content); // 更新主文档的最新内容

        mongoTemplate.save(resume);
        return commitId;
    }

    /**
     * 创建分支
     */
    public void createBranch(String resumeId, Long userId, String branchName) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null) {
            throw new RuntimeException("简历不存在");
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
        if (resume == null) {
            throw new RuntimeException("简历不存在");
        }

        if (!resume.getBranches().containsKey(branchName)) {
            throw new RuntimeException("分支不存在: " + branchName);
        }

        String commitId = resume.getBranches().get(branchName);
        if (commitId == null) {
            throw new RuntimeException("分支 " + branchName + " 没有关联的提交");
        }

        Resume commit = getCommitById(resumeId, userId, commitId);
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
        if (resume == null) {
            throw new RuntimeException("简历不存在");
        }

        String sourceCommitId = resume.getBranches().get(sourceBranch);
        String targetCommitId = resume.getBranches().get(targetBranch);

        if (sourceCommitId == null || targetCommitId == null) {
            throw new RuntimeException("源分支或目标分支不存在");
        }

        Resume sourceCommit = getCommitById(resumeId, userId, sourceCommitId);
        Resume targetCommit = getCommitById(resumeId, userId, targetCommitId);

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
    public List<Resume> getCommitHistory(String resumeId, Long userId) {
        Query query = new Query(Criteria.where("resume_id").is(resumeId)
                .and("user_id").is(userId)
                .and("doc_type").is("commit"));
        query.with(Sort.by(Sort.Direction.DESC, "commit_time"));
        return mongoTemplate.find(query, Resume.class);
    }

    /**
     * 检出到指定提交
     */
    public void checkout(String resumeId, Long userId, String commitId) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null) {
            throw new RuntimeException("简历不存在");
        }

        Resume commit = getCommitById(resumeId, userId, commitId);
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
        if (resume == null) {
            throw new RuntimeException("简历不存在");
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

    /**
     * 重置到指定提交（Hard Reset）
     * 这会将当前分支指针和内容都重置到指定提交
     */
    public void resetToCommit(String resumeId, Long userId, String commitId) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null) {
            throw new RuntimeException("简历不存在");
        }

        Resume targetCommit = getCommitById(resumeId, userId, commitId);
        if (targetCommit == null) {
            throw new RuntimeException("目标提交不存在: " + commitId);
        }

        // 验证目标提交是否在当前分支的历史中
        if (!isCommitInBranchHistory(resumeId, userId, resume.getCurrentBranch(), commitId)) {
            throw new RuntimeException("目标提交不在当前分支的历史中，无法重置");
        }

        // 重置分支指针
        resume.getBranches().put(resume.getCurrentBranch(), commitId);
        resume.setHeadCommit(commitId);
        resume.setContent(targetCommit.getContent());

        mongoTemplate.save(resume);
        log.info("重置分支 {} 到提交 {}", resume.getCurrentBranch(), commitId);
    }

    /**
     * 回滚指定提交（创建一个新提交来撤销指定提交的更改）
     */
    public String revertCommit(String resumeId, Long userId, String commitId) {
        log.info("开始回滚操作 - 简历: {}, 用户: {}, 提交: {}", resumeId, userId, commitId);

        Resume resume = getResumeById(resumeId, userId);
        if (resume == null) {
            log.error("简历不存在 - resumeId: {}, userId: {}", resumeId, userId);
            throw new RuntimeException("简历不存在");
        }

        if (resume.getHeadCommit() == null) {
            log.error("HEAD提交为空 - resumeId: {}", resumeId);
            throw new RuntimeException("HEAD提交为空，无法执行回滚操作");
        }

        Resume targetCommit = getCommitById(resumeId, userId, commitId);
        if (targetCommit == null) {
            log.error("要回滚的提交不存在 - commitId: {}", commitId);
            throw new RuntimeException("要回滚的提交不存在: " + commitId);
        }

        log.info("找到目标提交 - 消息: {}, 分支: {}, 父提交数量: {}",
                targetCommit.getMessage(), targetCommit.getBranch(), targetCommit.getParentCommits().size());

        // 检查是否为初始提交
        if (targetCommit.getParentCommits().isEmpty()) {
            log.warn("尝试回滚初始提交 - commitId: {}, 消息: {}", commitId, targetCommit.getMessage());
            throw new RuntimeException("无法回滚初始提交，初始提交没有父提交可以回溯到");
        }

        String parentCommitId = targetCommit.getParentCommits().get(0);
        log.info("获取父提交 - parentCommitId: {}", parentCommitId);

        Resume parentCommit = getCommitById(resumeId, userId, parentCommitId);
        if (parentCommit == null) {
            log.error("父提交不存在 - parentCommitId: {}, targetCommitId: {}", parentCommitId, commitId);
            throw new RuntimeException("父提交数据损坏，无法执行回滚操作。父提交ID: " + parentCommitId);
        }

        log.info("找到父提交 - 消息: {}, 分支: {}", parentCommit.getMessage(), parentCommit.getBranch());

        // 创建回滚提交
        String revertMessage = String.format("Revert \"%s\"\n\nThis reverts commit %s.",
                targetCommit.getMessage(), commitId.substring(0, 8));

        List<String> parentCommits = new ArrayList<>();
        parentCommits.add(resume.getHeadCommit());

        log.info("创建回滚提交 - 当前HEAD: {}, 回滚消息: {}", resume.getHeadCommit(), revertMessage);

        try {
            String revertCommitId = createCommit(resumeId, userId, revertMessage,
                    parentCommit.getContent(), resume.getCurrentBranch(), parentCommits, "revert");

            log.info("回滚提交创建成功 - revertCommitId: {}", revertCommitId);

            // 更新分支指针
            resume.getBranches().put(resume.getCurrentBranch(), revertCommitId);
            resume.setHeadCommit(revertCommitId);
            resume.setContent(parentCommit.getContent());

            mongoTemplate.save(resume);
            log.info("回滚操作完成 - 原提交: {}, 新提交: {}, 分支: {}",
                    commitId, revertCommitId, resume.getCurrentBranch());
            return revertCommitId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建回滚提交失败 - resumeId: {}, commitId: {}, error: {}",
                    resumeId, commitId, e.getMessage(), e);
            throw new RuntimeException("创建回滚提交失败: " + e.getMessage(), e);
        }
    }

    /**
     * 快速回溯到最近的N次提交
     */
    public List<Resume> getRecentCommits(String resumeId, Long userId, int limit) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null) {
            throw new RuntimeException("简历不存在");
        }

        // 从当前HEAD开始获取最近的提交
        List<Resume> recentCommits = new ArrayList<>();
        String currentCommitId = resume.getHeadCommit();

        while (currentCommitId != null && recentCommits.size() < limit) {
            Resume commit = getCommitById(resumeId, userId, currentCommitId);
            if (commit == null)
                break;

            recentCommits.add(commit);

            // 获取父提交（简单处理，只取第一个父提交）
            if (!commit.getParentCommits().isEmpty()) {
                currentCommitId = commit.getParentCommits().get(0);
            } else {
                break;
            }
        }

        return recentCommits;
    }

    /**
     * 获取指定分支的提交差异
     */
    public List<Resume> getCommitsBetween(String resumeId, Long userId, String fromCommitId, String toCommitId) {
        getCommitById(resumeId, userId, fromCommitId);
        getCommitById(resumeId, userId, toCommitId);
        List<Resume> commits = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String currentCommitId = toCommitId;

        while (currentCommitId != null && !currentCommitId.equals(fromCommitId) && visited.add(currentCommitId)) {
            Resume commit = getCommitById(resumeId, userId, currentCommitId);
            if (commit == null)
                break;

            commits.add(commit);

            // 获取父提交
            if (!commit.getParentCommits().isEmpty()) {
                currentCommitId = commit.getParentCommits().get(0);
            } else {
                break;
            }
        }

        return commits;
    }

    /**
     * 检查提交是否在指定分支的历史中
     */
    private boolean isCommitInBranchHistory(String resumeId, Long userId, String branchName, String commitId) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null)
            return false;

        String branchHead = resume.getBranches().get(branchName);
        if (branchHead == null)
            return false;

        // 从分支HEAD开始遍历历史
        String currentCommitId = branchHead;
        Set<String> visited = new HashSet<>();

        while (currentCommitId != null && !visited.contains(currentCommitId)) {
            if (currentCommitId.equals(commitId)) {
                return true;
            }

            visited.add(currentCommitId);
            Resume commit = getCommitById(resumeId, userId, currentCommitId);
            if (commit == null || commit.getParentCommits().isEmpty()) {
                break;
            }

            currentCommitId = commit.getParentCommits().get(0);
        }

        return false;
    }

    /**
     * 创建回溯操作记录（用于审计和撤销）
     */
    public void recordRollbackOperation(String resumeId, Long userId, String operationType,
            String fromCommitId, String toCommitId, String description) {
        Map<String, Object> rollbackRecord = new HashMap<>();
        rollbackRecord.put("operationType", operationType);
        rollbackRecord.put("fromCommit", fromCommitId);
        rollbackRecord.put("toCommit", toCommitId);
        rollbackRecord.put("description", description);
        rollbackRecord.put("timestamp", new Date());
        rollbackRecord.put("userId", userId);

        // 这里可以存储到专门的回溯历史集合中
        // 暂时先记录日志
        log.info("回溯操作记录 - 简历: {}, 操作: {}, 从: {} 到: {}, 描述: {}",
                resumeId, operationType, fromCommitId, toCommitId, description);
    }

    /**
     * 调试接口：获取指定提交的详细信息
     */
    public Map<String, Object> getCommitDebugInfo(String resumeId, Long userId, String commitId) {
        Map<String, Object> debugInfo = new HashMap<>();

        try {
            Resume resume = getResumeById(resumeId, userId);
            debugInfo.put("resumeExists", resume != null);
            if (resume != null) {
                debugInfo.put("currentBranch", resume.getCurrentBranch());
                debugInfo.put("headCommit", resume.getHeadCommit());
            } else {
                debugInfo.put("currentBranch", null);
                debugInfo.put("headCommit", null);
            }

            Resume commit = getCommitById(resumeId, userId, commitId);
            debugInfo.put("commitExists", commit != null);

            if (commit != null) {
                debugInfo.put("commitMessage", commit.getMessage());
                debugInfo.put("commitBranch", commit.getBranch());
                debugInfo.put("commitType", commit.getCommitType());
                debugInfo.put("commitTime", commit.getCommitTime());
                debugInfo.put("parentCommits", commit.getParentCommits());
                debugInfo.put("parentCount", commit.getParentCommits().size());
                debugInfo.put("isInitialCommit", commit.getParentCommits().isEmpty());

                // 检查父提交是否存在
                List<Map<String, Object>> parentInfo = new ArrayList<>();
                for (String parentId : commit.getParentCommits()) {
                    Map<String, Object> pInfo = new HashMap<>();
                    Resume parentCommit = getCommitById(resumeId, userId, parentId);
                    pInfo.put("parentId", parentId);
                    pInfo.put("parentExists", parentCommit != null);
                    if (parentCommit != null) {
                        pInfo.put("parentMessage", parentCommit.getMessage());
                        pInfo.put("parentTime", parentCommit.getCommitTime());
                    }
                    parentInfo.add(pInfo);
                }
                debugInfo.put("parentCommitsInfo", parentInfo);
            }

            debugInfo.put("success", true);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            debugInfo.put("success", false);
            debugInfo.put("error", e.getMessage());
            log.error("获取提交调试信息失败", e);
        }

        return debugInfo;
    }

    /**
     * 调试接口：验证版本控制数据完整性
     */
    public Map<String, Object> validateVersionControlIntegrity(String resumeId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            Resume resume = getResumeById(resumeId, userId);
            if (resume == null) {
                errors.add("简历不存在");
                result.put("errors", errors);
                return result;
            }

            if (resume.getHeadCommit() == null) {
                errors.add("HEAD提交为空");
            } else {
                Resume headCommit = getCommitById(resumeId, userId, resume.getHeadCommit());
                if (headCommit == null) {
                    errors.add("HEAD提交不存在: " + resume.getHeadCommit());
                }
            }

            // 检查分支
            if (resume.getBranches().isEmpty()) {
                errors.add("没有分支信息");
            } else {
                for (Map.Entry<String, String> branch : resume.getBranches().entrySet()) {
                    String branchName = branch.getKey();
                    String commitId = branch.getValue();

                    if (commitId == null) {
                        errors.add("分支 " + branchName + " 没有关联的提交");
                        continue;
                    }

                    Resume commit = getCommitById(resumeId, userId, commitId);
                    if (commit == null) {
                        errors.add("分支 " + branchName + " 的提交不存在: " + commitId);
                    }
                }
            }

            // 检查当前分支
            if (resume.getCurrentBranch() == null) {
                errors.add("当前分支为空");
            } else if (!resume.getBranches().containsKey(resume.getCurrentBranch())) {
                errors.add("当前分支不在分支列表中: " + resume.getCurrentBranch());
            }

            // 获取所有提交并检查完整性
            List<Resume> allCommits = getCommitHistory(resumeId, userId);
            result.put("totalCommits", allCommits.size());

            int orphanCommits = 0;
            int corruptedCommits = 0;

            for (Resume commit : allCommits) {
                // 检查父提交是否存在
                for (String parentId : commit.getParentCommits()) {
                    Resume parent = getCommitById(resumeId, userId, parentId);
                    if (parent == null) {
                        corruptedCommits++;
                        warnings.add("提交 " + commit.getId() + " 的父提交不存在: " + parentId);
                    }
                }

                // 检查是否为孤立提交（除了被分支引用）
                boolean isReferenced = false;
                for (String branchCommit : resume.getBranches().values()) {
                    if (commit.getId().equals(branchCommit)) {
                        isReferenced = true;
                        break;
                    }
                }
                if (!isReferenced && !commit.getParentCommits().isEmpty()) {
                    // 检查是否被其他提交引用为父提交
                    boolean isParent = allCommits.stream()
                            .anyMatch(c -> c.getParentCommits().contains(commit.getId()));
                    if (!isParent) {
                        orphanCommits++;
                        warnings.add("发现孤立提交: " + commit.getId());
                    }
                }
            }

            result.put("orphanCommits", orphanCommits);
            result.put("corruptedCommits", corruptedCommits);
            result.put("branchCount", resume.getBranches().size());
            result.put("stashCount", resume.getStashStack().size());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            errors.add("验证过程中发生异常: " + e.getMessage());
            log.error("验证版本控制完整性失败", e);
        }

        result.put("errors", errors);
        result.put("warnings", warnings);
        result.put("isValid", errors.isEmpty());

        return result;
    }

    /**
     * 修复重复的提交ID问题
     */
    public Map<String, Object> fixDuplicateCommitIds(String resumeId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        List<String> actions = new ArrayList<>();

        try {
            Resume resume = getResumeById(resumeId, userId);
            if (resume == null) {
                throw new RuntimeException("简历不存在");
            }

            // 获取所有提交
            List<Resume> allCommits = getCommitHistory(resumeId, userId);
            actions.add("找到 " + allCommits.size() + " 个提交");

            // 检查是否有重复ID
            Map<String, Integer> idCount = new HashMap<>();
            for (Resume commit : allCommits) {
                idCount.put(commit.getId(), idCount.getOrDefault(commit.getId(), 0) + 1);
            }

            List<String> duplicateIds = idCount.entrySet().stream()
                    .filter(entry -> entry.getValue() > 1)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (duplicateIds.isEmpty()) {
                result.put("success", true);
                result.put("message", "没有发现重复的提交ID");
                result.put("actions", actions);
                return result;
            }

            actions.add("发现重复ID: " + duplicateIds);

            // 删除所有旧的提交
            Query deleteQuery = new Query(Criteria.where("resume_id").is(resumeId).and("user_id").is(userId));
            long deletedCount = mongoTemplate.remove(deleteQuery, Resume.class).getDeletedCount();
            actions.add("删除了 " + deletedCount + " 个重复提交");

            // 重新创建初始提交
            String initialContent = resume.getContent() != null ? resume.getContent() : "# 新建简历\n\n在此处开始编辑...";
            String initialCommitId = createCommit(resumeId, userId, "Initial commit",
                    initialContent, "main", new ArrayList<>(), "normal");
            actions.add("创建新的初始提交: " + initialCommitId);

            // 更新简历的版本控制信息
            resume.setCurrentBranch("main");
            resume.setHeadCommit(initialCommitId);
            resume.setRepositoryId(UUID.randomUUID().toString());
            resume.getBranches().clear();
            resume.getBranches().put("main", initialCommitId);
            resume.getStashStack().clear();

            mongoTemplate.save(resume);
            actions.add("更新了简历的版本控制状态");

            result.put("success", true);
            result.put("message", "成功修复重复ID问题");
            result.put("actions", actions);
            result.put("initialCommitId", initialCommitId);
            result.put("duplicateIdsCount", duplicateIds.size());

            log.info("🔧 修复重复ID完成 - 简历: {}, 重复ID数量: {}, 新初始提交: {}",
                    resumeId, duplicateIds.size(), initialCommitId);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("actions", actions);
            log.error("修复重复ID失败", e);
        }

        return result;
    }

    // 辅助方法
    private Resume getResumeById(String resumeId, Long userId) {
        Query query = new Query(Criteria.where("id").is(resumeId)
                .and("userId").is(userId)
                .and("doc_type").is("meta"));
        return mongoTemplate.findOne(query, Resume.class);
    }

    private Resume getCommitById(String resumeId, Long userId, String commitId) {
        Query query = new Query(Criteria.where("_id").is(commitId).and("doc_type").is("commit")
                .and("resume_id").is(resumeId).and("user_id").is(userId));
        Resume commit = mongoTemplate.findOne(query, Resume.class);
        if (commit == null) {
            throw new BusinessException(404, "提交不存在");
        }
        return commit;
    }

    private String generateCommitId() {
        String generatedId = "commit-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        log.info("🔧 生成新的提交ID: {}", generatedId);
        return generatedId;
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