package com.david.hlp.web.resume;

import com.david.hlp.commons.utils.RedisLockUtil;
import com.david.hlp.web.common.exception.GlobalExceptionHandler;
import com.david.hlp.web.resume.controller.ResumeController;
import com.david.hlp.web.resume.entity.Resume;
import com.david.hlp.web.resume.service.ResumeService;
import com.david.hlp.web.resume.service.ResumeVersionControlService;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ResumeAppearanceTest {
    private static final String RESOURCE = "/api/resume/legacy";
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final Document stored = new Document("_id", "legacy").append("user_id", 1L)
            .append("doc_type", "meta").append("title", "简历").append("content", "# 原文")
            .append("head_commit", "head").append("workspace_revision", 4L);
    private final ResumeVersionControlService versions =
            new ResumeVersionControlService(mongo, mock(RedisLockUtil.class));
    private MockMvc api;
    private long currentUser = 1L;

    @BeforeEach
    void setUp() {
        var converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, new MongoMappingContext());
        converter.afterPropertiesSet();
        ResumeController controller = new ResumeController(new ResumeService(mongo, versions)) {
            @Override
            protected Long getCurrentUserId() { return currentUser; }
        };
        api = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        when(mongo.findOne(any(Query.class), eq(Resume.class))).thenAnswer(call -> converter.read(Resume.class, stored));
    }

    @Test
    void rejectsAnUnknownThemeWith400() throws Exception {
        api.perform(patch(RESOURCE + "/appearance").contentType("application/json")
                .content("{\"themeId\":\"neon\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(mongo);
    }

    @Test
    void rejectsAnotherOwnerWith403() throws Exception {
        currentUser = 2L;
        when(mongo.updateFirst(any(Query.class), any(UpdateDefinition.class), eq(Resume.class)))
                .thenAnswer(call -> {
                    Query owner = call.getArgument(0);
                    assertEquals(new Document("id", "legacy").append("userId", 2L).append("docType", "meta"),
                            owner.getQueryObject());
                    return UpdateResult.acknowledged(0, 0L, null);
                });
        api.perform(patch(RESOURCE + "/appearance").contentType("application/json")
                .content("{\"themeId\":\"modern\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void legacyRecordWithoutAThemeReadsAsBasic() throws Exception {
        api.perform(get(RESOURCE)).andExpect(status().isOk()).andExpect(jsonPath("$.data.themeId").value("basic"));
    }

    @Test
    void reopeningUsesTheNewThemeWithoutChangingTheWorkspaceOrHistory() throws Exception {
        Resume snapshot = new Resume();
        snapshot.setId("head");
        when(mongo.find(any(Query.class), eq(Resume.class))).thenReturn(List.of(snapshot));
        when(mongo.updateFirst(any(Query.class), any(UpdateDefinition.class), eq(Resume.class)))
                .thenAnswer(call -> {
                    UpdateDefinition update = call.getArgument(1);
                    var fields = update.getUpdateObject();
                    assertEquals(new Document("$set", new Document("themeId", "modern")), fields);
                    stored.put("theme_id", "modern");
                    return UpdateResult.acknowledged(1, 1L, null);
                });
        var before = versions.getCommitHistory("legacy", 1L);

        api.perform(patch(RESOURCE + "/appearance").contentType("application/json")
                .content("{\"themeId\":\"modern\",\"content\":\"ignored\",\"workspaceRevision\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.themeId").value("modern"));
        api.perform(get(RESOURCE)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.themeId").value("modern"))
                .andExpect(jsonPath("$.data.content").value("# 原文"))
                .andExpect(jsonPath("$.data.headCommit").value("head"))
                .andExpect(jsonPath("$.data.workspaceRevision").value(4));
        assertEquals(before, versions.getCommitHistory("legacy", 1L));
        verify(mongo, never()).insert(any(Resume.class));
    }
}
