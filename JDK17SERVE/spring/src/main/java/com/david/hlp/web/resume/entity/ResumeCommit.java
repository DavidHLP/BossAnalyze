package com.david.hlp.web.resume.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * 简历提交历史实体类
 * 类似Git的commit对象，记录每次提交的信息
 */
@Data
@Document(collection = "resume_commits")
public class ResumeCommit implements Serializable {

    @Id
    private String id; // commit ID

    @Field("resume_id")
    private String resumeId; // 关联的简历ID

    @Field("user_id")
    private Long userId;

    @Field("message")
    private String message; // 提交信息

    @Field("content")
    private String content; // 提交时的内容快照

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

    @CreatedDate
    @Field("commit_time")
    private Date commitTime;

    @Field("tree_hash")
    private String treeHash; // 内容哈希，用于快速比较

    @Field("metadata")
    private java.util.Map<String, Object> metadata = new java.util.HashMap<>(); // 额外元数据
}