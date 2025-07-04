package com.david.hlp.web.resume.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历实体类，用于在同一集合中存储简历元数据（meta）和提交历史（commit）
 * 通过 'doc_type' 字段区分
 */
@Data
@Document(collection = "resumes")
public class Resume implements Serializable {

    @Id
    private String id;

    @Field("user_id")
    private Long userId;

    @Field("content")
    private String content; // 对于 "meta" 是当前内容, "commit" 是快照

    @Field("doc_type")
    private String docType; // "meta" or "commit"

    // "meta" type fields
    @Field("title")
    private String title;

    @CreatedDate
    @Field("created_at")
    private Date createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Date updatedAt;

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

    // "commit" type fields
    @Field("resume_id")
    private String resumeId; // 关联的简历ID

    @Field("message")
    private String message; // 提交信息

    @Field("branch")
    private String branch; // 所属分支

    @Field("parent_commits")
    private List<String> parentCommits = new ArrayList<>(); // 父提交ID列表（支持merge）

    @Field("commit_type")
    private String commitType = "normal"; // normal, merge, cherry-pick

    @Field("author")
    private String author; // 提交者

    @Field("author_email")
    private String authorEmail;

    @Field("commit_time")
    private Date commitTime;

    @Field("tree_hash")
    private String treeHash; // 内容哈希，用于快速比较

    @Field("metadata")
    private Map<String, Object> metadata = new HashMap<>(); // 额外元数据
}