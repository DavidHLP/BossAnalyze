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
import java.util.List;
import java.util.ArrayList;

/**
 * 简历主实体类（包含版本控制元数据）
 * 每个文档代表一份简历的元信息和当前状态
 */
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

    @Field("branches")
    private Map<String, String> branches = new HashMap<>(); // branch -> commitId

    @Field("stash_stack")
    private List<Map<String, Object>> stashStack = new ArrayList<>();

    @Field("doc_type")
    private String docType = "meta"; // 文档类型，用于在同一集合中区分
}