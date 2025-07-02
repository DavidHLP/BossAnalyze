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
}
