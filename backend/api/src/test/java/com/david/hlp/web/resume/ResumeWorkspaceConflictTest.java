package com.david.hlp.web.resume;

import com.david.hlp.commons.utils.RedisLockUtil;
import com.david.hlp.web.common.exception.GlobalExceptionHandler;
import com.david.hlp.web.resume.controller.ResumeVersionController;
import com.david.hlp.web.resume.entity.Resume;
import com.david.hlp.web.resume.service.ResumeVersionControlService;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ResumeWorkspaceConflictTest {
    @Test
    void staleSaveReturnsCurrentWorkspaceWithoutWritingASnapshot() throws Exception {
        MongoTemplate mongo = mock(MongoTemplate.class);
        Resume existing = new Resume();
        existing.setHeadCommit("current-head");
        when(mongo.findOne(any(Query.class), eq(Resume.class))).thenReturn(existing);
        RedisLockUtil lock = mock(RedisLockUtil.class);
        when(lock.tryLock(anyString(), anyLong(), anyLong(), any())).thenReturn(true);
        ResumeVersionController controller = new ResumeVersionController(new ResumeVersionControlService(mongo, lock)) {
            @Override
            protected Long getCurrentUserId() { return 1L; }
        };
        var api = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();

        api.perform(post("/api/resume/mine/version/commit").contentType("application/json")
                .content("{\"message\":\"save\",\"content\":\"stale content\",\"workspaceRevision\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.data.workspaceRevision").value(0))
                .andExpect(jsonPath("$.data.headCommit").value("current-head"));
        verify(mongo, never()).insert(any(Resume.class));
    }
}
