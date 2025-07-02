package com.david.hlp.web.resume.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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

    // Git-like 版本控制字段
    @Field("current_branch")
    private String currentBranch = "main";

    @Field("head_commit_id")
    private String headCommitId;

    @Field("branches")
    private List<Branch> branches = new ArrayList<>();

    @Field("commits")
    private List<Commit> commits = new ArrayList<>();

    @Field("tags")
    private List<Tag> tags = new ArrayList<>();

    @CreatedDate
    @Field("created_at")
    private Date createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Date updatedAt;

    /**
     * Git分支概念
     */
    @Data
    public static class Branch implements Serializable {
        @Field("name")
        private String name;

        @Field("head_commit_id")
        private String headCommitId;

        @Field("description")
        private String description;

        @Field("created_at")
        private Date createdAt;

        @Field("is_default")
        private Boolean isDefault = false;

        @Field("created_by")
        private String createdBy;
    }

    /**
     * Git提交概念
     */
    @Data
    public static class Commit implements Serializable {
        @Field("commit_id")
        private String commitId;

        @Field("parent_commit_ids")
        private List<String> parentCommitIds = new ArrayList<>();

        @Field("branch")
        private String branch;

        @Field("title")
        private String title;

        @Field("content")
        private String content;

        @Field("commit_message")
        private String commitMessage;

        @Field("author")
        private String author;

        @Field("commit_time")
        private Date commitTime;

        @Field("changes_summary")
        private String changesSummary;

        @Field("is_merge_commit")
        private Boolean isMergeCommit = false;

        // 生成类似Git的短SHA
        public static String generateCommitId() {
            try {
                String uuid = UUID.randomUUID().toString();
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                byte[] digest = md.digest(uuid.getBytes());
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(digest.length, 4); i++) {
                    sb.append(String.format("%02x", digest[i]));
                }
                return sb.toString();
            } catch (Exception e) {
                return UUID.randomUUID().toString().substring(0, 8);
            }
        }
    }

    /**
     * Git标签概念
     */
    @Data
    public static class Tag implements Serializable {
        @Field("name")
        private String name;

        @Field("commit_id")
        private String commitId;

        @Field("message")
        private String message;

        @Field("created_at")
        private Date createdAt;

        @Field("created_by")
        private String createdBy;

        @Field("tag_type")
        private String tagType; // lightweight, annotated
    }

    /**
     * 文件差异信息
     */
    @Data
    public static class FileDiff implements Serializable {
        @Field("field_name")
        private String fieldName;

        @Field("old_value")
        private String oldValue;

        @Field("new_value")
        private String newValue;

        @Field("change_type")
        private String changeType; // ADD, MODIFY, DELETE
    }

    // ================== 请求 DTO 类 ==================

    /**
     * 提交请求
     */
    @Data
    public static class CommitRequest implements Serializable {
        private String title;
        private String content;
        private String message;
        private String author = "user";
    }

    /**
     * 分支请求
     */
    @Data
    public static class BranchRequest implements Serializable {
        private String name;
        private String description;
        private String fromCommitId;
    }

    /**
     * 合并请求
     */
    @Data
    public static class MergeRequest implements Serializable {
        private String sourceBranch;
        private String targetBranch;
        private String message;
    }

    /**
     * 标签请求
     */
    @Data
    public static class TagRequest implements Serializable {
        private String name;
        private String commitId;
        private String message;
    }
}