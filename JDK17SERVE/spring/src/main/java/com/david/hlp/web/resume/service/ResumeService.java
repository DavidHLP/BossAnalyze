package com.david.hlp.web.resume.service;

import com.david.hlp.commons.utils.RedisCacheHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import com.david.hlp.web.resume.entity.Resume;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 简历服务类 - Git风格版本管理
 * 支持分支、提交、合并、标签等Git核心功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final MongoTemplate mongoTemplate;
    private final RedisCacheHelper redisCacheHelper;

    // ================== 基础CRUD操作 ==================

    public List<Resume> getResumesByUserId(Long userId) {
        Query query = new Query(Criteria.where("user_id").is(userId));
        return mongoTemplate.find(query, Resume.class);
    }

    public Resume getResumeById(String id, Long userId) {
        Resume resume = mongoTemplate.findById(id, Resume.class);
        if (resume == null || !Objects.equals(resume.getUserId(), userId)) {
            return null;
        }
        return resume;
    }

    public Resume createResume(Resume resume, Long userId) {
        resume.setUserId(userId);
        resume.setCreatedAt(new Date());
        resume.setUpdatedAt(new Date());

        // 初始化Git仓库
        initializeGitRepository(resume);

        Resume savedResume = mongoTemplate.save(resume);
        log.info("创建简历成功: resumeId={}, userId={}, 初始分支: {}",
                savedResume.getId(), userId, savedResume.getCurrentBranch());
        return savedResume;
    }

    public Resume updateResume(String id, Resume resumeDetails, Long userId) {
        Resume existingResume = getResumeById(id, userId);
        if (existingResume == null) {
            return null;
        }

        // 检查内容是否发生变化
        boolean contentChanged = !Objects.equals(existingResume.getTitle(), resumeDetails.getTitle()) ||
                !Objects.equals(existingResume.getContent(), resumeDetails.getContent());

        if (contentChanged) {
            // 自动提交变更 (类似 git add + git commit)
            String commitMessage = "自动保存: 更新简历内容";
            commitChanges(existingResume, resumeDetails.getTitle(), resumeDetails.getContent(),
                    commitMessage, "system", false);
        } else {
            // 仅更新时间戳
            existingResume.setUpdatedAt(new Date());
        }

        Resume savedResume = mongoTemplate.save(existingResume);
        log.info("更新简历成功: resumeId={}, 当前分支: {}, HEAD: {}",
                savedResume.getId(), savedResume.getCurrentBranch(), savedResume.getHeadCommitId());
        return savedResume;
    }

    public void deleteResume(String id, Long userId) {
        Resume existingResume = getResumeById(id, userId);
        if (existingResume != null) {
            mongoTemplate.remove(existingResume);
            log.info("删除简历成功: resumeId={}, userId={}", id, userId);
        }
    }

    // ================== Git风格版本管理操作 ==================

    /**
     * 获取提交历史 (git log)
     */
    public List<Resume.Commit> getCommitHistory(String resumeId, String branch, Long userId) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null) {
            return Collections.emptyList();
        }

        String targetBranch = branch != null ? branch : resume.getCurrentBranch();

        return resume.getCommits()
                .stream()
                .filter(commit -> targetBranch.equals(commit.getBranch()))
                .sorted((c1, c2) -> c2.getCommitTime().compareTo(c1.getCommitTime()))
                .collect(Collectors.toList());
    }

    /**
     * 获取特定提交 (git show)
     */
    public Resume.Commit getCommit(String resumeId, String commitId, Long userId) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null) {
            return null;
        }

        return resume.getCommits()
                .stream()
                .filter(commit -> commitId.equals(commit.getCommitId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 手动提交变更 (git commit)
     */
    public Resume.Commit commitChanges(String resumeId, String title, String content,
            String commitMessage, String author, Long userId) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null) {
            throw new RuntimeException("简历不存在");
        }

        return commitChanges(resume, title, content, commitMessage, author, true);
    }

    /**
     * 重置到指定提交 (git reset --hard)
     * 将HEAD重置到指定提交，并只保留所有分支可达的提交记录。
     */
    public Resume resetToCommit(String resumeId, String commitId, Long userId) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null) {
            throw new RuntimeException("简历不存在");
        }

        Resume.Commit targetCommit = resume.getCommits().stream()
                .filter(c -> commitId.equals(c.getCommitId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("指定提交不存在"));

        // 1. 先移动当前分支的HEAD指针到目标提交
        String currentBranchName = resume.getCurrentBranch();
        updateBranchHead(resume, currentBranchName, targetCommit.getCommitId());

        // 2. 重新计算所有分支的可达提交，实现垃圾回收
        Set<String> reachableCommitIds = new HashSet<>();
        Map<String, Resume.Commit> allCommitsMap = resume.getCommits().stream()
                .collect(Collectors.toMap(Resume.Commit::getCommitId, c -> c, (a, b) -> a));

        for (Resume.Branch branch : resume.getBranches()) {
            Resume.Commit current = allCommitsMap.get(branch.getHeadCommitId());
            while (current != null) {
                if (reachableCommitIds.contains(current.getCommitId()))
                    break;
                reachableCommitIds.add(current.getCommitId());

                // 追溯父节点，对于合并提交，默认追溯第一个父节点
                List<String> parentIds = current.getParentCommitIds();
                if (parentIds == null || parentIds.isEmpty())
                    break;
                current = allCommitsMap.get(parentIds.get(0));
            }
        }

        // 3. 过滤提交列表，只保留可达的提交
        List<Resume.Commit> newCommitList = resume.getCommits().stream()
                .filter(commit -> reachableCommitIds.contains(commit.getCommitId()))
                .collect(Collectors.toList());
        resume.setCommits(newCommitList);

        // 4. 更新工作区内容
        resume.setTitle(targetCommit.getTitle());
        resume.setContent(targetCommit.getContent());
        resume.setHeadCommitId(targetCommit.getCommitId());
        resume.setUpdatedAt(new Date());

        Resume savedResume = mongoTemplate.save(resume);
        log.info("硬重置到提交成功 (hard reset): resumeId={}, commitId={}, branch={}",
                resumeId, commitId, resume.getCurrentBranch());
        return savedResume;
    }

    // ================== Git分支管理 ==================

    /**
     * 获取所有分支 (git branch -a)
     */
    public List<Resume.Branch> getBranches(String resumeId, Long userId) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(resume.getBranches());
    }

    /**
     * 创建新分支 (git branch)
     */
    public Resume.Branch createBranch(String resumeId, String branchName, String description,
            String fromCommitId, Long userId) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null) {
            throw new RuntimeException("简历不存在");
        }

        // 检查分支名是否已存在
        boolean branchExists = resume.getBranches()
                .stream()
                .anyMatch(branch -> branchName.equals(branch.getName()));

        if (branchExists) {
            throw new RuntimeException("分支已存在: " + branchName);
        }

        String baseCommitId = fromCommitId != null && !fromCommitId.trim().isEmpty()
                ? fromCommitId
                : resume.getHeadCommitId();

        // **安全加固**：强制检查基础提交是否存在，防止创建悬空分支
        String finalBaseCommitId = baseCommitId;
        boolean commitExists = resume.getCommits().stream()
                .anyMatch(c -> c.getCommitId().equals(finalBaseCommitId));
        if (!commitExists) {
            throw new RuntimeException("无法创建分支，因为基础提交不存在: " + baseCommitId);
        }

        Resume.Branch newBranch = new Resume.Branch();
        newBranch.setName(branchName);
        newBranch.setHeadCommitId(baseCommitId);
        newBranch.setDescription(description);
        newBranch.setCreatedAt(new Date());
        newBranch.setCreatedBy("user");
        newBranch.setIsDefault(false);

        resume.getBranches().add(newBranch);
        mongoTemplate.save(resume);

        log.info("创建分支成功: resumeId={}, branchName={}, baseCommit={}",
                resumeId, branchName, baseCommitId);
        return newBranch;
    }

    /**
     * 切换分支 (git checkout)
     * 在切换时，如果新分支是首次检出，会为其复制父分支的历史。
     */
    public Resume checkoutBranch(String resumeId, String branchName, Long userId) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null) {
            throw new RuntimeException("简历不存在");
        }

        Resume.Branch targetBranch = findBranch(resume, branchName);

        // 检查新分支是否还没有任何属于它自己的提交
        long countCommitsOnBranch = resume.getCommits().stream()
                .filter(c -> branchName.equals(c.getBranch()))
                .count();

        // 如果是首次检出（没有自己的提交），则从父分支复制历史
        if (countCommitsOnBranch == 0) {
            Map<String, Resume.Commit> allCommitsMap = resume.getCommits().stream()
                    .collect(Collectors.toMap(Resume.Commit::getCommitId, c -> c, (a, b) -> a));

            Resume.Commit baseCommit = allCommitsMap.get(targetBranch.getHeadCommitId());
            if (baseCommit == null) {
                throw new RuntimeException("基础提交不存在，无法复制历史: " + targetBranch.getHeadCommitId());
            }

            // 追溯并复制历史
            List<Resume.Commit> historyToCopy = new ArrayList<>();
            Resume.Commit current = baseCommit;
            while (current != null) {
                // 创建一个新的提交对象，但修改其分支名为新分支名
                Resume.Commit newCommitForBranch = deepCopyCommit(current);
                newCommitForBranch.setBranch(branchName);
                historyToCopy.add(newCommitForBranch);

                // 默认追溯第一个父节点
                if (current.getParentCommitIds() == null || current.getParentCommitIds().isEmpty())
                    break;
                current = allCommitsMap.get(current.getParentCommitIds().get(0));
            }

            // 将复制的历史添加到总提交列表中
            resume.getCommits().addAll(historyToCopy);
        }

        // 获取分支最新的提交内容
        Resume.Commit headCommit = getCommitById(resume, targetBranch.getHeadCommitId());

        // 切换到目标分支
        resume.setCurrentBranch(branchName);
        resume.setHeadCommitId(targetBranch.getHeadCommitId());
        resume.setTitle(headCommit.getTitle());
        resume.setContent(headCommit.getContent());
        resume.setUpdatedAt(new Date());

        Resume savedResume = mongoTemplate.save(resume);
        log.info("切换分支成功: resumeId={}, targetBranch={}, HEAD={}",
                resumeId, branchName, targetBranch.getHeadCommitId());
        return savedResume;
    }

    /**
     * 合并分支 (git merge)
     */
    public Resume mergeBranch(String resumeId, String sourceBranch, String targetBranch,
            String mergeMessage, Long userId) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null) {
            throw new RuntimeException("简历不存在");
        }

        Resume.Branch source = findBranch(resume, sourceBranch);
        Resume.Branch target = findBranch(resume, targetBranch);

        Resume.Commit sourceCommit = getCommitById(resume, source.getHeadCommitId());

        // 创建合并提交
        String commitId = Resume.Commit.generateCommitId();
        Resume.Commit mergeCommit = new Resume.Commit();
        mergeCommit.setCommitId(commitId);
        // 合并提交有两个父节点：1. 目标分支的HEAD 2. 源分支的HEAD
        mergeCommit.setParentCommitIds(Arrays.asList(target.getHeadCommitId(), source.getHeadCommitId()));
        mergeCommit.setBranch(targetBranch);
        mergeCommit.setTitle(sourceCommit.getTitle()); // 简化策略：使用源分支的内容
        mergeCommit.setContent(sourceCommit.getContent());
        mergeCommit.setCommitMessage(
                mergeMessage != null ? mergeMessage : String.format("合并分支 '%s' 到 '%s'", sourceBranch, targetBranch));
        mergeCommit.setAuthor("system");
        mergeCommit.setCommitTime(new Date());
        mergeCommit.setIsMergeCommit(true);
        mergeCommit.setChangesSummary(String.format("合并 %s -> %s", sourceBranch, targetBranch));

        resume.getCommits().add(mergeCommit);

        // 更新目标分支HEAD
        updateBranchHead(resume, targetBranch, commitId);

        // 如果当前在目标分支，更新工作区内容
        if (targetBranch.equals(resume.getCurrentBranch())) {
            resume.setHeadCommitId(commitId);
            resume.setTitle(sourceCommit.getTitle());
            resume.setContent(sourceCommit.getContent());
        }

        resume.setUpdatedAt(new Date());
        Resume savedResume = mongoTemplate.save(resume);

        log.info("合并分支成功: resumeId={}, {} -> {}, mergeCommit={}",
                resumeId, sourceBranch, targetBranch, commitId);
        return savedResume;
    }

    // ================== Git标签管理 ==================

    /**
     * 创建标签 (git tag)
     */
    public Resume.Tag createTag(String resumeId, String tagName, String commitId,
            String message, Long userId) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null) {
            throw new RuntimeException("简历不存在");
        }

        // 检查标签是否已存在
        boolean tagExists = resume.getTags()
                .stream()
                .anyMatch(tag -> tagName.equals(tag.getName()));

        if (tagExists) {
            throw new RuntimeException("标签已存在: " + tagName);
        }

        String targetCommitId = commitId != null ? commitId : resume.getHeadCommitId();

        Resume.Tag newTag = new Resume.Tag();
        newTag.setName(tagName);
        newTag.setCommitId(targetCommitId);
        newTag.setMessage(message);
        newTag.setCreatedAt(new Date());
        newTag.setCreatedBy("user");
        newTag.setTagType("annotated");

        resume.getTags().add(newTag);
        mongoTemplate.save(resume);

        log.info("创建标签成功: resumeId={}, tagName={}, commitId={}",
                resumeId, tagName, targetCommitId);
        return newTag;
    }

    /**
     * 获取所有标签 (git tag -l)
     */
    public List<Resume.Tag> getTags(String resumeId, Long userId) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null) {
            return Collections.emptyList();
        }

        return resume.getTags()
                .stream()
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    /**
     * 删除分支 (git branch -d)
     * 只有非当前、非默认的分支才能被删除。
     */
    public void deleteBranch(String resumeId, String branchName, Long userId) {
        Resume resume = getResumeById(resumeId, userId);
        if (resume == null) {
            throw new RuntimeException("简历不存在");
        }

        // 安全检查
        if ("main".equals(branchName) || resume.getCurrentBranch().equals(branchName)) {
            throw new RuntimeException("不能删除main分支或当前所在的分支");
        }

        Optional<Resume.Branch> branchToRemove = resume.getBranches().stream()
                .filter(b -> b.getName().equals(branchName))
                .findFirst();

        if (branchToRemove.isEmpty()) {
            throw new RuntimeException("要删除的分支不存在");
        }

        if (branchToRemove.get().getIsDefault()) {
            throw new RuntimeException("不能删除默认分支");
        }

        resume.getBranches().removeIf(b -> b.getName().equals(branchName));

        // 注意：我们在这里不清理提交，让 cleanupOldCommits 机制来处理孤儿提交
        mongoTemplate.save(resume);
        log.info("删除分支成功: resumeId={}, branchName={}", resumeId, branchName);
    }

    // ================== 私有辅助方法 ==================

    /**
     * 初始化Git仓库
     */
    private void initializeGitRepository(Resume resume) {
        // 创建main分支
        Resume.Branch mainBranch = new Resume.Branch();
        mainBranch.setName("main");
        mainBranch.setDescription("主分支");
        mainBranch.setCreatedAt(new Date());
        mainBranch.setCreatedBy("system");
        mainBranch.setIsDefault(true);

        // 创建初始提交
        String initialCommitId = Resume.Commit.generateCommitId();
        Resume.Commit initialCommit = new Resume.Commit();
        initialCommit.setCommitId(initialCommitId);
        initialCommit.setParentCommitIds(Collections.emptyList());
        initialCommit.setBranch("main");
        initialCommit.setTitle(resume.getTitle());
        initialCommit.setContent(resume.getContent());
        initialCommit.setCommitMessage("初始提交: 创建简历");
        initialCommit.setAuthor("system");
        initialCommit.setCommitTime(new Date());
        initialCommit.setChangesSummary("创建新简历");
        initialCommit.setIsMergeCommit(false);

        // 设置分支HEAD
        mainBranch.setHeadCommitId(initialCommitId);

        // 设置简历状态
        resume.setCurrentBranch("main");
        resume.setHeadCommitId(initialCommitId);
        resume.getBranches().add(mainBranch);
        resume.getCommits().add(initialCommit);
    }

    /**
     * 提交变更的核心逻辑
     */
    private Resume.Commit commitChanges(Resume resume, String title, String content,
            String commitMessage, String author, boolean save) {
        String commitId = Resume.Commit.generateCommitId();

        Resume.Commit newCommit = new Resume.Commit();
        newCommit.setCommitId(commitId);
        newCommit.setParentCommitIds(Collections.singletonList(resume.getHeadCommitId())); // 普通提交只有一个父节点
        newCommit.setBranch(resume.getCurrentBranch());
        newCommit.setTitle(title);
        newCommit.setContent(content);
        newCommit.setCommitMessage(commitMessage);
        newCommit.setAuthor(author);
        newCommit.setCommitTime(new Date());
        newCommit.setChangesSummary(generateChangesSummary(resume, title, content));
        newCommit.setIsMergeCommit(false);

        resume.getCommits().add(newCommit);

        // 更新工作区内容
        resume.setTitle(title);
        resume.setContent(content);
        resume.setHeadCommitId(commitId);
        resume.setUpdatedAt(new Date());

        // 更新当前分支的HEAD
        updateBranchHead(resume, resume.getCurrentBranch(), commitId);

        // 限制提交历史数量
        cleanupOldCommits(resume, 50);

        if (save) {
            mongoTemplate.save(resume);
        }

        log.info("提交变更成功: resumeId={}, commitId={}, branch={}, message={}",
                resume.getId(), commitId, resume.getCurrentBranch(), commitMessage);
        return newCommit;
    }

    /**
     * 更新分支HEAD指针
     */
    private void updateBranchHead(Resume resume, String branchName, String commitId) {
        resume.getBranches()
                .stream()
                .filter(branch -> branchName.equals(branch.getName()))
                .findFirst()
                .ifPresent(branch -> branch.setHeadCommitId(commitId));
    }

    /**
     * 查找分支
     */
    private Resume.Branch findBranch(Resume resume, String branchName) {
        return resume.getBranches()
                .stream()
                .filter(branch -> branchName.equals(branch.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("分支不存在: " + branchName));
    }

    /**
     * 根据ID获取提交
     */
    private Resume.Commit getCommitById(Resume resume, String commitId) {
        return resume.getCommits()
                .stream()
                .filter(commit -> commitId.equals(commit.getCommitId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("提交不存在: " + commitId));
    }

    /**
     * 生成变更摘要
     */
    private String generateChangesSummary(Resume resume, String newTitle, String newContent) {
        List<String> changes = new ArrayList<>();

        if (!Objects.equals(resume.getTitle(), newTitle)) {
            changes.add("标题变更");
        }
        if (!Objects.equals(resume.getContent(), newContent)) {
            changes.add("内容变更");
        }

        return changes.isEmpty() ? "无变更" : String.join(", ", changes);
    }

    /**
     * 清理旧提交（保留最近的指定数量，但只清理不可达的提交）
     */
    private void cleanupOldCommits(Resume resume, int keepCount) {
        List<Resume.Commit> commits = resume.getCommits();
        if (commits.size() <= keepCount) {
            return;
        }

        // 1. 找到所有可达的提交
        Set<String> reachableCommitIds = new HashSet<>();
        Map<String, Resume.Commit> allCommitsMap = commits.stream()
                .collect(Collectors.toMap(Resume.Commit::getCommitId, c -> c, (a, b) -> a));

        for (Resume.Branch branch : resume.getBranches()) {
            Resume.Commit current = allCommitsMap.get(branch.getHeadCommitId());
            while (current != null) {
                if (reachableCommitIds.contains(current.getCommitId()))
                    break;
                reachableCommitIds.add(current.getCommitId());

                // 追溯父节点，对于合并提交，默认追溯第一个父节点
                List<String> parentIds = current.getParentCommitIds();
                if (parentIds == null || parentIds.isEmpty())
                    break;
                current = allCommitsMap.get(parentIds.get(0));
            }
        }

        // 2. 识别出不可达的提交，并按时间排序
        List<Resume.Commit> unreachableCommits = commits.stream()
                .filter(c -> !reachableCommitIds.contains(c.getCommitId()))
                .sorted((c1, c2) -> c1.getCommitTime().compareTo(c2.getCommitTime())) // 最旧的在前
                .collect(Collectors.toList());

        // 3. 计算需要移除的提交数量
        int numToRemove = commits.size() - keepCount;
        if (numToRemove <= 0)
            return;

        // 4. 从不可达的提交中选出要移除的
        Set<String> commitsToRemove = unreachableCommits.stream()
                .limit(Math.min(numToRemove, unreachableCommits.size()))
                .map(Resume.Commit::getCommitId)
                .collect(Collectors.toSet());

        if (commitsToRemove.isEmpty())
            return;

        List<Resume.Commit> newCommitList = commits.stream()
                .filter(c -> !commitsToRemove.contains(c.getCommitId()))
                .collect(Collectors.toList());

        resume.setCommits(newCommitList);
        log.debug("清理旧提交: resumeId={}, 删除了{}个不可达提交", resume.getId(), commitsToRemove.size());
    }

    /**
     * 深度复制提交
     */
    private Resume.Commit deepCopyCommit(Resume.Commit commit) {
        Resume.Commit newCommit = new Resume.Commit();
        newCommit.setCommitId(commit.getCommitId());
        newCommit.setParentCommitIds(new ArrayList<>(commit.getParentCommitIds()));
        newCommit.setBranch(commit.getBranch());
        newCommit.setTitle(commit.getTitle());
        newCommit.setContent(commit.getContent());
        newCommit.setCommitMessage(commit.getCommitMessage());
        newCommit.setAuthor(commit.getAuthor());
        newCommit.setCommitTime(commit.getCommitTime());
        newCommit.setChangesSummary(commit.getChangesSummary());
        newCommit.setIsMergeCommit(commit.getIsMergeCommit());
        return newCommit;
    }
}
