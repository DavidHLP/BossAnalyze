package com.david.hlp.web.resume.controller;

import com.david.hlp.web.common.controller.BaseController;
import com.david.hlp.web.common.entity.Result;
import com.david.hlp.web.resume.entity.Resume;
import com.david.hlp.web.resume.entity.ResumeCommit;
import com.david.hlp.web.resume.service.ResumeVersionControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    /**
     * 重置到指定提交
     */
    @PostMapping("/reset")
    public Result<Void> resetToCommit(@PathVariable String resumeId,
            @RequestBody Map<String, String> request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }

        try {
            String commitId = request.get("commitId");
            if (commitId == null || commitId.trim().isEmpty()) {
                return Result.error(HttpStatus.BAD_REQUEST.value(), "提交ID不能为空");
            }

            versionControlService.resetToCommit(resumeId, userId, commitId);

            // 记录重置操作
            versionControlService.recordRollbackOperation(resumeId, userId, "RESET",
                    "HEAD", commitId, "重置分支到指定提交");

            return Result.success(null);
        } catch (Exception e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    /**
     * 回滚指定提交
     */
    @PostMapping("/revert")
    public Result<String> revertCommit(@PathVariable String resumeId,
            @RequestBody Map<String, String> request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }

        try {
            String commitId = request.get("commitId");
            if (commitId == null || commitId.trim().isEmpty()) {
                return Result.error(HttpStatus.BAD_REQUEST.value(), "提交ID不能为空");
            }

            String revertCommitId = versionControlService.revertCommit(resumeId, userId, commitId);

            // 记录回滚操作
            versionControlService.recordRollbackOperation(resumeId, userId, "REVERT",
                    commitId, revertCommitId, "回滚指定提交的更改");

            return Result.success(revertCommitId);
        } catch (Exception e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    /**
     * 获取最近的提交记录（用于快速回溯）
     */
    @GetMapping("/recent")
    public Result<List<ResumeCommit>> getRecentCommits(@PathVariable String resumeId,
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }

        try {
            if (limit <= 0 || limit > 50) {
                limit = 10; // 默认限制
            }

            List<ResumeCommit> recentCommits = versionControlService.getRecentCommits(resumeId, userId, limit);
            return Result.success(recentCommits);
        } catch (Exception e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    /**
     * 获取两个提交之间的差异
     */
    @GetMapping("/diff")
    public Result<List<ResumeCommit>> getCommitsBetween(@PathVariable String resumeId,
            @RequestParam String fromCommit,
            @RequestParam String toCommit) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }

        try {
            List<ResumeCommit> commits = versionControlService.getCommitsBetween(resumeId, userId, fromCommit,
                    toCommit);
            return Result.success(commits);
        } catch (Exception e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    /**
     * 调试接口：获取指定提交的详细信息
     */
    @GetMapping("/debug/commit/{commitId}")
    public Result<Map<String, Object>> getCommitDebugInfo(@PathVariable String resumeId,
            @PathVariable String commitId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }

        try {
            Map<String, Object> debugInfo = versionControlService.getCommitDebugInfo(resumeId, userId, commitId);
            return Result.success(debugInfo);
        } catch (Exception e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    /**
     * 调试接口：验证版本控制数据完整性
     */
    @GetMapping("/debug/validate")
    public Result<Map<String, Object>> validateVersionControl(@PathVariable String resumeId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }

        try {
            Map<String, Object> validationResult = versionControlService.validateVersionControlIntegrity(resumeId,
                    userId);
            return Result.success(validationResult);
        } catch (Exception e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    /**
     * 清理并重新初始化版本控制（修复ID重复问题）
     */
    @PostMapping("/fix-duplicate-ids")
    public Result<Map<String, Object>> fixDuplicateIds(@PathVariable String resumeId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }

        try {
            Map<String, Object> result = versionControlService.fixDuplicateCommitIds(resumeId, userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("修复重复ID失败: {}", e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "修复失败: " + e.getMessage());
        }
    }
}