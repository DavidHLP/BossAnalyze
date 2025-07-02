package com.david.hlp.web.resume.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.Date;

/**
 * 简历版本实体类
 * 用于存储简历的历史版本，支持版本回溯功能
 */
@Data
@Document(collection = "resume_versions")
public class ResumeVersion implements Serializable {

    @Id
    private String id;

    @Field("resume_id")
    private String resumeId;

    @Field("user_id")
    private Long userId;

    @Field("version_number")
    private Integer versionNumber;

    @Field("title")
    private String title;

    @Field("content")
    private String content;

    @Field("change_description")
    private String changeDescription;

    @Field("is_auto_save")
    private Boolean isAutoSave = false;

    @CreatedDate
    @Field("created_at")
    private Date createdAt;

    @Field("created_by")
    private String createdBy = "system";
}