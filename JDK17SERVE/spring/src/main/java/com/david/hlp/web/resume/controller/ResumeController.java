package com.david.hlp.web.resume.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.david.hlp.web.common.controller.BaseController;
import com.david.hlp.web.common.entity.Result;
import com.david.hlp.web.resume.entity.Resume;
import com.david.hlp.web.resume.service.ResumeService;
import org.springframework.http.HttpStatus;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController extends BaseController {

    private final ResumeService resumeService;

    @GetMapping
    public Result<List<Resume>> getResumes() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }
        return Result.success(resumeService.getResumesByUserId(userId));
    }

    @GetMapping("/{id}")
    public Result<Resume> getResumeById(@PathVariable String id) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }
        Resume resume = resumeService.getResumeById(id, userId);
        return resume != null ? Result.success(resume) : Result.error(HttpStatus.NOT_FOUND.value(), "简历不存在");
    }

    @PostMapping
    public Result<Resume> createResume(@RequestBody Resume resume) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }
        return Result.success(resumeService.createResume(resume, userId));
    }

    @PutMapping("/{id}")
    public Result<Resume> updateResume(@PathVariable String id, @RequestBody Resume resume) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }
        Resume updatedResume = resumeService.updateResume(id, resume, userId);
        return updatedResume != null ? Result.success(updatedResume)
                : Result.error(HttpStatus.NOT_FOUND.value(), "简历不存在");
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteResume(@PathVariable String id) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }
        resumeService.deleteResume(id, userId);
        return Result.success(null);
    }

    // ================== Git风格版本管理 API ==================

    /**
     * 获取提交历史 (git log)
     */
    @GetMapping("/{id}/commits")
    public Result<List<Resume.Commit>> getCommitHistory(@PathVariable String id,
            @RequestParam(required = false) String branch) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }
        List<Resume.Commit> commits = resumeService.getCommitHistory(id, branch, userId);
        return Result.success(commits);
    }

    /**
     * 获取特定提交 (git show)
     */
    @GetMapping("/{id}/commits/{commitId}")
    public Result<Resume.Commit> getCommit(@PathVariable String id, @PathVariable String commitId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }
        Resume.Commit commit = resumeService.getCommit(id, commitId, userId);
        return commit != null ? Result.success(commit)
                : Result.error(HttpStatus.NOT_FOUND.value(), "提交不存在");
    }

    /**
     * 手动提交变更 (git commit)
     */
    @PostMapping("/{id}/commit")
    public Result<Resume.Commit> commitChanges(@PathVariable String id,
            @RequestBody Resume.CommitRequest request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }
        try {
            Resume.Commit commit = resumeService.commitChanges(id, request.getTitle(),
                    request.getContent(), request.getMessage(), request.getAuthor(), userId);
            return Result.success(commit);
        } catch (RuntimeException e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    /**
     * 重置到指定提交 (git reset)
     */
    @PostMapping("/{id}/reset/{commitId}")
    public Result<Resume> resetToCommit(@PathVariable String id, @PathVariable String commitId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }
        try {
            Resume resume = resumeService.resetToCommit(id, commitId, userId);
            return Result.success(resume);
        } catch (RuntimeException e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    // ================== Git分支管理 API ==================

    /**
     * 获取所有分支 (git branch -a)
     */
    @GetMapping("/{id}/branches")
    public Result<List<Resume.Branch>> getBranches(@PathVariable String id) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }
        List<Resume.Branch> branches = resumeService.getBranches(id, userId);
        return Result.success(branches);
    }

    /**
     * 创建新分支 (git branch)
     */
    @PostMapping("/{id}/branches")
    public Result<Resume.Branch> createBranch(@PathVariable String id,
            @RequestBody Resume.BranchRequest request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }
        try {
            Resume.Branch branch = resumeService.createBranch(id, request.getName(),
                    request.getDescription(), request.getFromCommitId(), userId);
            return Result.success(branch);
        } catch (RuntimeException e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    /**
     * 切换分支 (git checkout)
     */
    @PostMapping("/{id}/checkout/{branchName}")
    public Result<Resume> checkoutBranch(@PathVariable String id, @PathVariable String branchName) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }
        try {
            Resume resume = resumeService.checkoutBranch(id, branchName, userId);
            return Result.success(resume);
        } catch (RuntimeException e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    /**
     * 合并分支 (git merge)
     */
    @PostMapping("/{id}/merge")
    public Result<Resume> mergeBranch(@PathVariable String id,
            @RequestBody Resume.MergeRequest request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }
        try {
            Resume resume = resumeService.mergeBranch(id, request.getSourceBranch(),
                    request.getTargetBranch(), request.getMessage(), userId);
            return Result.success(resume);
        } catch (RuntimeException e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    /**
     * 删除分支 (git branch -d)
     */
    @DeleteMapping("/{id}/branches/{branchName}")
    public Result<Void> deleteBranch(@PathVariable String id, @PathVariable String branchName) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }
        try {
            resumeService.deleteBranch(id, branchName, userId);
            return Result.success(null);
        } catch (RuntimeException e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    // ================== Git标签管理 API ==================

    /**
     * 获取所有标签 (git tag -l)
     */
    @GetMapping("/{id}/tags")
    public Result<List<Resume.Tag>> getTags(@PathVariable String id) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }
        List<Resume.Tag> tags = resumeService.getTags(id, userId);
        return Result.success(tags);
    }

    /**
     * 创建标签 (git tag)
     */
    @PostMapping("/{id}/tags")
    public Result<Resume.Tag> createTag(@PathVariable String id,
            @RequestBody Resume.TagRequest request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }
        try {
            Resume.Tag tag = resumeService.createTag(id, request.getName(),
                    request.getCommitId(), request.getMessage(), userId);
            return Result.success(tag);
        } catch (RuntimeException e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }
}
