package com.david.hlp.web.resume.controller;

import com.david.hlp.web.common.controller.BaseController;
import com.david.hlp.web.common.entity.Result;
import com.david.hlp.web.resume.entity.Resume;
import com.david.hlp.web.resume.entity.ResumeCommit;
import com.david.hlp.web.resume.service.ResumeVersionControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 简历版本控制API控制器
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/resume/{resumeId}/version")
@RequiredArgsConstructor
public class ResumeVersionController extends BaseController {

    private final ResumeVersionControlService versionControlService;

    /**
     * 初始化版本控制
     */
    @PostMapping("/init")
    public Result<Resume> initVersionControl(@PathVariable String resumeId,
            @RequestBody Map<String, String> request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }

        try {
            String initialContent = request.getOrDefault("content", "");
            Resume resume = versionControlService.initVersionControl(resumeId, userId, initialContent);
            return Result.success(resume);
        } catch (Exception e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    /**
     * 提交更改
     */
    @PostMapping("/commit")
    public Result<String> commit(@PathVariable String resumeId,
            @RequestBody Map<String, String> request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }

        try {
            String message = request.get("message");
            String content = request.get("content");

            if (message == null || message.trim().isEmpty()) {
                return Result.error(HttpStatus.BAD_REQUEST.value(), "提交信息不能为空");
            }

            String commitId = versionControlService.commit(resumeId, userId, message, content);
            return Result.success(commitId);
        } catch (Exception e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    /**
     * 创建分支
     */
    @PostMapping("/branch")
    public Result<Void> createBranch(@PathVariable String resumeId,
            @RequestBody Map<String, String> request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }

        try {
            String branchName = request.get("name");
            if (branchName == null || branchName.trim().isEmpty()) {
                return Result.error(HttpStatus.BAD_REQUEST.value(), "分支名称不能为空");
            }

            versionControlService.createBranch(resumeId, userId, branchName);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    /**
     * 切换分支
     */
    @PostMapping("/switch")
    public Result<Void> switchBranch(@PathVariable String resumeId,
            @RequestBody Map<String, String> request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }

        try {
            String branchName = request.get("branch");
            versionControlService.switchBranch(resumeId, userId, branchName);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    /**
     * 合并分支
     */
    @PostMapping("/merge")
    public Result<String> mergeBranch(@PathVariable String resumeId,
            @RequestBody Map<String, String> request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }

        try {
            String sourceBranch = request.get("source");
            String targetBranch = request.get("target");

            String mergeCommitId = versionControlService.mergeBranch(resumeId, userId, sourceBranch, targetBranch);
            return Result.success(mergeCommitId);
        } catch (Exception e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    /**
     * 获取提交历史
     */
    @GetMapping("/history")
    public Result<List<ResumeCommit>> getCommitHistory(@PathVariable String resumeId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }

        try {
            List<ResumeCommit> history = versionControlService.getCommitHistory(resumeId, userId);
            return Result.success(history);
        } catch (Exception e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    /**
     * 检出到指定提交
     */
    @PostMapping("/checkout")
    public Result<Void> checkout(@PathVariable String resumeId,
            @RequestBody Map<String, String> request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }

        try {
            String commitId = request.get("commitId");
            versionControlService.checkout(resumeId, userId, commitId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    /**
     * 储藏当前更改
     */
    @PostMapping("/stash")
    public Result<Void> stash(@PathVariable String resumeId,
            @RequestBody Map<String, String> request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }

        try {
            String content = request.get("content");
            versionControlService.stash(resumeId, userId, content);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    /**
     * 弹出储藏
     */
    @PostMapping("/stash/pop")
    public Result<String> stashPop(@PathVariable String resumeId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }

        try {
            String content = versionControlService.stashPop(resumeId, userId);
            return Result.success(content);
        } catch (Exception e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }
}