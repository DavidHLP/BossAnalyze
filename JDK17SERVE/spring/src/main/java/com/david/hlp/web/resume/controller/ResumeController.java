package com.david.hlp.web.resume.controller;

import com.david.hlp.web.common.enums.ResultCode;
import com.david.hlp.web.common.controller.BaseController;
import com.david.hlp.web.common.entity.PageInfo;
import com.david.hlp.web.common.entity.Result;
import com.david.hlp.web.resume.service.ResumeService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController extends BaseController {
        private static final String RESOURCE_NOT_FOUND = "简历不存在";
        private final ResumeService resumeService;

        @GetMapping("/list")
        public Result<PageInfo<Map<String, Object>>> getAllResumes(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String name,
                        @RequestParam(required = false) String experience,
                        @RequestParam(required = false) String jobTarget) {

                log.debug("查询简历列表，参数: page={}, size={}, name={}, experience={}, jobTarget={}",
                                page, size, name, experience, jobTarget);

                return Result.success(resumeService.searchResumes(
                                getCurrentUserId(),
                                PageRequest.of(page, size),
                                name,
                                experience,
                                jobTarget));
        }

        @GetMapping("/get")
        public Result<Map<String, Object>> getResumeById(
                        @RequestParam @NotBlank(message = "ID不能为空") String id) {
                return resumeService.findByIdAsJson(id)
                                .map(Result::success)
                                .orElse(Result.error(ResultCode.NOT_FOUND, RESOURCE_NOT_FOUND));
        }

        @PostMapping
        public Result<Map<String, Object>> createResume(
                        @RequestBody Map<String, Object> resumeData) {
                resumeData.remove("id");
                return Result.success(resumeService.saveAsJson(getCurrentUserId(), resumeData));
        }

        @PutMapping
        public Result<Map<String, Object>> updateResume(
                        @RequestBody Map<String, Object> resumeData) {
                String id = (String) resumeData.get("id");
                if (id == null || id.isBlank()) {
                        return Result.error(ResultCode.PARAM_ERROR, "ID不能为空");
                }

                return resumeService.updateAsJson(id, getCurrentUserId(), resumeData)
                                .map(Result::success)
                                .orElse(Result.error(ResultCode.NOT_FOUND, RESOURCE_NOT_FOUND));
        }

        @DeleteMapping
        public Result<Void> deleteResume(
                        @RequestParam @NotBlank(message = "ID不能为空") String id) {
                if (resumeService.deleteById(id, getCurrentUserId())) {
                        return Result.success(null);
                }
                return Result.error(ResultCode.NOT_FOUND, RESOURCE_NOT_FOUND);
        }
}
