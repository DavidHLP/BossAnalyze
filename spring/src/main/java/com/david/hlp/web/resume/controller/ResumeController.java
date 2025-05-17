package com.david.hlp.web.resume.controller;

import com.david.hlp.web.resume.entity.Resume;
import com.david.hlp.web.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.david.hlp.web.common.result.PageInfo;
import com.david.hlp.web.common.result.Result;
import com.david.hlp.web.common.enums.ResultCode;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {
        private final ResumeService resumeService;

        @GetMapping("/list")
        public Result<PageInfo<Resume>> getAllResumes(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                Pageable pageable = PageRequest.of(page, size);
                return Result.<PageInfo<Resume>>builder()
                                .code(ResultCode.SUCCESS.getCode())
                                .message(ResultCode.SUCCESS.getMessage())
                                .data(PageInfo.of(resumeService.findAll(pageable)))
                                .build();
        }

        @GetMapping("/get")
        public Result<Resume> getResumeById(@RequestParam String id) {
                return resumeService.findById(id)
                                .map(resume -> Result.<Resume>builder()
                                                .code(ResultCode.SUCCESS.getCode())
                                                .message(ResultCode.SUCCESS.getMessage())
                                                .data(resume)
                                                .build())
                                .orElse(Result.error(ResultCode.NOT_FOUND));
        }

        @PostMapping("/create")
        public Result<Resume> createResume(@RequestBody Resume resume) {
                return Result.<Resume>builder()
                                .code(ResultCode.SUCCESS.getCode())
                                .message(ResultCode.SUCCESS.getMessage())
                                .data(resumeService.save(resume))
                                .build();
        }

        @PutMapping("/update")
        public Result<Resume> updateResume(@RequestBody Resume resume) {
                return resumeService.findById(resume.getId())
                                .map(existingResume -> {
                                        return Result.<Resume>builder()
                                                        .code(ResultCode.SUCCESS.getCode())
                                                        .message(ResultCode.SUCCESS.getMessage())
                                                        .data(resumeService.save(resume))
                                                        .build();
                                })
                                .orElse(Result.error(ResultCode.NOT_FOUND));
        }

        @DeleteMapping("/delete")
        public Result<Void> deleteResume(@RequestParam String id) {
                return resumeService.findById(id)
                                .map(resume -> {
                                        resumeService.deleteById(id);
                                        return Result.<Void>builder()
                                                        .code(ResultCode.SUCCESS.getCode())
                                                        .message(ResultCode.SUCCESS.getMessage())
                                                        .data(null)
                                                        .build();
                                })
                                .orElse(Result.error(ResultCode.NOT_FOUND));
        }

        /**
         * 根据用户ID查询所有简历
         */
        @GetMapping("/user")
        public Result<List<Resume>> getResumesByUserId(@RequestParam String userId) {
                return Result.<List<Resume>>builder()
                                .code(ResultCode.SUCCESS.getCode())
                                .message(ResultCode.SUCCESS.getMessage())
                                .data(resumeService.findByUserId(userId))
                                .build();
        }

        /**
         * 根据用户ID查询最新创建的简历
         */
        @GetMapping("/user/latest")
        public Result<List<Resume>> getLatestResumesByUserId(@RequestParam String userId) {
                return Result.<List<Resume>>builder()
                                .code(ResultCode.SUCCESS.getCode())
                                .message(ResultCode.SUCCESS.getMessage())
                                .data(resumeService.findLatestByUserId(userId))
                                .build();
        }

        /**
         * 获取用户最新创建的一份简历
         */
        @GetMapping("/user/latest/one")
        public Result<Resume> getLatestOneResumeByUserId(@RequestParam String userId) {
                return resumeService.findLatestOneByUserId(userId)
                                .map(resume -> Result.<Resume>builder()
                                                .code(ResultCode.SUCCESS.getCode())
                                                .message(ResultCode.SUCCESS.getMessage())
                                                .data(resume)
                                                .build())
                                .orElse(Result.error(ResultCode.NOT_FOUND));
        }

        /**
         * 根据用户ID查询最近更新的简历
         */
        @GetMapping("/user/recently-updated")
        public Result<List<Resume>> getRecentlyUpdatedResumesByUserId(@RequestParam String userId) {
                return Result.<List<Resume>>builder()
                                .code(ResultCode.SUCCESS.getCode())
                                .message(ResultCode.SUCCESS.getMessage())
                                .data(resumeService.findRecentlyUpdatedByUserId(userId))
                                .build();
        }

        /**
         * 获取用户最近更新的一份简历
         */
        @GetMapping("/user/recently-updated/one")
        public Result<Resume> getRecentlyUpdatedOneResumeByUserId(@RequestParam String userId) {
                return resumeService.findRecentlyUpdatedOneByUserId(userId)
                                .map(resume -> Result.<Resume>builder()
                                                .code(ResultCode.SUCCESS.getCode())
                                                .message(ResultCode.SUCCESS.getMessage())
                                                .data(resume)
                                                .build())
                                .orElse(Result.error(ResultCode.NOT_FOUND));
        }

        /**
         * 分页查询用户简历
         */
        @GetMapping("/user/page")
        public Result<Page<Resume>> getResumesByUserIdPaged(
                        @RequestParam String userId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                Pageable pageable = PageRequest.of(page, size);
                return Result.<Page<Resume>>builder()
                                .code(ResultCode.SUCCESS.getCode())
                                .message(ResultCode.SUCCESS.getMessage())
                                .data(resumeService.findByUserId(userId, pageable))
                                .build();
        }

        /**
         * 分页查询用户简历（按用户名）
         */
        @GetMapping("/username/page")
        public Result<Page<Resume>> getResumesByUserNamePaged(
                        @RequestParam String userName,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                Pageable pageable = PageRequest.of(page, size);
                return Result.<Page<Resume>>builder()
                                .code(ResultCode.SUCCESS.getCode())
                                .message(ResultCode.SUCCESS.getMessage())
                                .data(resumeService.findByUserName(userName, pageable))
                                .build();
        }
}
