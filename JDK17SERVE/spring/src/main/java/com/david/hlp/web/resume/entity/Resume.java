package com.david.hlp.web.resume.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

@Data
@Document(collection = "resumes")
public class Resume implements Serializable {

    @Id
    private String id;

    @Field("user_id")
    private Long userId;

    @Field("title")
    private String title;

    @Field("content")
    private String content;

    @CreatedDate
    @Field("created_at")
    private Date createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Date updatedAt;

    // 版本控制相关字段
    @Field("current_branch")
    private String currentBranch = "main";

    @Field("head_commit")
    private String headCommit;

    @Field("repository_id")
    private String repositoryId;

    @Field("has_version_control")
    private Boolean hasVersionControl = false;

    @Field("branches")
    private Map<String, String> branches = new HashMap<>(); // branch -> commitId

    @Field("stash_stack")
    private java.util.List<Map<String, Object>> stashStack = new java.util.ArrayList<>();
}