package com.david.hlp.web.resume.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.david.hlp.web.common.controller.BaseController;
import com.david.hlp.web.common.entity.Result;
import com.david.hlp.web.resume.entity.Resume;
import com.david.hlp.web.resume.entity.ResumeVersion;
import com.david.hlp.web.resume.service.ResumeService;
import com.david.hlp.web.resume.service.ResumeVersionService;
import org.springframework.http.HttpStatus;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController extends BaseController {

    private final ResumeService resumeService;
    private final ResumeVersionService resumeVersionService;

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

    // ================== 版本管理相关 API ==================

    /**
     * 获取简历版本历史
     */
    @GetMapping("/{id}/versions")
    public Result<List<ResumeVersion>> getVersionHistory(@PathVariable String id) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }
        List<ResumeVersion> versions = resumeVersionService.getVersionHistory(id, userId);
        return Result.success(versions);
    }

    /**
     * 获取特定版本内容
     */
    @GetMapping("/{id}/versions/{versionNumber}")
    public Result<ResumeVersion> getVersion(@PathVariable String id, @PathVariable Integer versionNumber) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }
        ResumeVersion version = resumeVersionService.getVersion(id, versionNumber, userId);
        return version != null ? Result.success(version)
                : Result.error(HttpStatus.NOT_FOUND.value(), "版本不存在");
    }

    /**
     * 恢复到指定版本
     */
    @PostMapping("/{id}/versions/{versionNumber}/restore")
    public Result<Resume> restoreToVersion(@PathVariable String id, @PathVariable Integer versionNumber) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }
        try {
            Resume restoredResume = resumeVersionService.restoreToVersion(id, versionNumber, userId);
            return Result.success(restoredResume);
        } catch (RuntimeException e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    /**
     * 手动创建版本快照
     */
    @PostMapping("/{id}/versions")
    public Result<ResumeVersion> createVersion(@PathVariable String id,
            @RequestParam(required = false) String description) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }

        Resume resume = resumeService.getResumeById(id, userId);
        if (resume == null) {
            return Result.error(HttpStatus.NOT_FOUND.value(), "简历不存在");
        }

        String changeDescription = description != null ? description : "手动创建快照";
        ResumeVersion version = resumeVersionService.createVersion(resume, changeDescription, false);
        return Result.success(version);
    }

    /**
     * 清理旧版本
     */
    @PostMapping("/{id}/versions/cleanup")
    public Result<Void> cleanupOldVersions(@PathVariable String id,
            @RequestParam(defaultValue = "10") int keepCount) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
        }

        resumeVersionService.cleanOldVersions(id, userId, keepCount);
        return Result.success(null);
    }
}
