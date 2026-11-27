package com.david.hlp.web.resume;

import com.david.hlp.commons.utils.RedisLockUtil;
import com.david.hlp.web.resume.entity.Resume;
import com.mongodb.client.result.UpdateResult;
import com.david.hlp.web.resume.controller.ResumeVersionController;
import com.david.hlp.web.resume.service.ResumeService;
import com.david.hlp.web.resume.service.ResumeVersionControlService;
import com.david.hlp.web.common.exception.BusinessException;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResumeIntegrityTest {
    @Test
    void creationDiscardsClientIdentityAndAllInternalMetadata() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        ResumeService service = new ResumeService(mongo, versions(mongo));
        Resume request = new Resume();
        request.setId("victim");
        request.setUserId(99L);
        request.setDocType("commit");
        request.setTitle("My resume");
        request.setContent("hello");
        request.setResumeId("victim-resume");
        request.setHeadCommit("foreign");
        request.setPublishedCommitId("foreign");
        request.setWorkspaceRevision(99);
        request.getBranches().put("evil", "foreign");
        request.getMetadata().put("internal", "forged");
        when(mongo.insert(any(Resume.class))).thenAnswer(call -> {
            Resume inserted = call.getArgument(0);
            assertEquals(1L, inserted.getUserId());
            assertNotEquals("victim", inserted.getId());
            assertEquals(0, inserted.getWorkspaceRevision());
            assertTrue(inserted.getMetadata().isEmpty());
            return inserted;
        });
        Resume created = service.createResume(request, 1L);
        assertEquals("meta", created.getDocType());
        assertNull(created.getResumeId());
        assertEquals("My resume", created.getTitle());
        assertEquals(Map.of("main", created.getHeadCommit()), created.getBranches());
        assertEquals(created.getHeadCommit(), created.getPublishedCommitId());
        assertEquals("victim", request.getId());
        assertNotNull(created.getRepositoryId());
        when(mongo.find(any(Query.class), eq(Resume.class))).thenAnswer(call -> {
            Document q = ((Query) call.getArgument(0)).getQueryObject();
            return created.getDocType().equals(q.get("docType")) && created.getUserId().equals(q.get("user_id"))
                    ? List.of(created) : List.of();
        });
        when(mongo.findOne(any(Query.class), eq(Resume.class))).thenAnswer(call -> {
            Document q = ((Query) call.getArgument(0)).getQueryObject();
            return created.getId().equals(q.get("id")) && created.getDocType().equals(q.get("docType"))
                    && created.getUserId().equals(q.get("userId")) ? created : null;
        });
        assertEquals(List.of(created), service.getResumesByUserId(1L));
        assertSame(created, service.getResumeById(created.getId(), 1L));
        assertNull(service.getResumeById(created.getId(), 2L));
    }

    @Test
    void titleOnlySaveKeepsContentAndUpdatesOwnedMeta() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        Resume existing = new Resume();
        existing.setId("mine");
        existing.setUserId(1L);
        existing.setContent("keep content");
        when(mongo.findOne(any(Query.class), eq(Resume.class))).thenReturn(existing);
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(Resume.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        Resume updated = new ResumeService(mongo, versions(mongo)).updateResume("mine", 1L, "renamed", null, null);
        assertEquals("renamed", updated.getTitle());
        assertEquals("keep content", updated.getContent());
        assertEquals(1, updated.getWorkspaceRevision());
    }

    @Test
    void versionStringResultsAreReturnedAsDataForFrontendCallers() {
        ResumeVersionControlService versions = mock(ResumeVersionControlService.class);
        ResumeVersionController controller = new ResumeVersionController(versions) {
            @Override
            protected Long getCurrentUserId() { return 1L; }
        };
        when(versions.commit("mine", 1L, "save", "new content", null)).thenReturn("commit-123");
        when(versions.mergeBranch("mine", 1L, "feature", "main", null)).thenReturn("merge-123");
        when(versions.stashPop("mine", 1L, null)).thenReturn("恢复的内容\nUnicode 简历");
        when(versions.revertCommit("mine", 1L, "commit-123", null)).thenReturn("revert-123");
        assertEquals("commit-123", controller.commit("mine", Map.of(
                "message", "save", "content", "new content")).getBody().getData());
        assertEquals("merge-123", controller.mergeBranch("mine", Map.of(
                "source", "feature", "target", "main")).getBody().getData());
        assertEquals("恢复的内容\nUnicode 简历", controller.stashPop("mine", null).getBody().getData());
        assertEquals("revert-123", controller.revertCommit("mine", Map.of(
                "commitId", "commit-123")).getBody().getData());
    }

    private ResumeVersionControlService versions(MongoTemplate mongo) {
        RedisLockUtil lock = mock(RedisLockUtil.class);
        when(lock.tryLock(anyString(), anyLong(), anyLong(), any())).thenReturn(true);
        return new ResumeVersionControlService(mongo, lock);
    }

    private MongoTemplate fixture(Resume meta, Resume... commits) {
        MongoTemplate mongo = mock(MongoTemplate.class);
        meta.setId("mine");
        meta.setUserId(1L);
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(Resume.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        when(mongo.findOne(any(Query.class), eq(Resume.class))).thenAnswer(call -> {
            Document q = ((Query) call.getArgument(0)).getQueryObject();
            if ("meta".equals(q.get("doc_type"))) return meta;
            // A missing predicate is treated as a wildcard, so the old global lookup leaks the fixture.
            for (Resume commit : commits) {
                if (commit.getId().equals(q.get("_id"))
                        && (!q.containsKey("user_id") || commit.getUserId().equals(q.get("user_id")))
                        && (!q.containsKey("resume_id") || commit.getResumeId().equals(q.get("resume_id")))) return commit;
            }
            return null;
        });
        return mongo;
    }

    private Resume commit(String id, String resumeId, Long userId, String... parents) {
        Resume c = new Resume();
        c.setId(id); c.setResumeId(resumeId); c.setUserId(userId);
        c.setContent("secret " + id); c.setParentCommits(List.of(parents));
        return c;
    }

    @Test
    void externalCommitIdsCannotCrossUserOrResumeOrMutateMeta() {
        for (Resume foreign : List.of(commit("foreign", "mine", 2L), commit("foreign", "other", 1L))) {
            Resume meta = new Resume(); meta.setContent("original"); meta.setHeadCommit("head");
            MongoTemplate mongo = fixture(meta, foreign);
            ResumeVersionControlService service = versions(mongo);
            assertThrows(BusinessException.class, () -> service.checkout("mine", 1L, "foreign", null));
            assertThrows(BusinessException.class, () -> service.resetToCommit("mine", 1L, "foreign", null));
            assertThrows(BusinessException.class, () -> service.revertCommit("mine", 1L, "foreign", null));
            assertThrows(BusinessException.class, () -> service.getCommitsBetween("mine", 1L, "foreign", "foreign"));
            assertThrows(BusinessException.class, () -> service.getCommitDebugInfo("mine", 1L, "foreign"));
            assertEquals("original", meta.getContent());
            assertEquals("head", meta.getHeadCommit());
            verify(mongo, never()).updateFirst(any(Query.class), any(Update.class), eq(Resume.class));
            verify(mongo, never()).insert(any(Resume.class));
        }
    }

    @Test
    void recursiveParentsAndStoredBranchPointersRemainScoped() {
        Resume meta = new Resume(); meta.setHeadCommit("owned-head"); meta.setContent("original");
        meta.getBranches().put("main", "owned-head"); meta.getBranches().put("foreign", "foreign-parent");
        MongoTemplate mongo = fixture(meta, commit("owned-head", "mine", 1L, "foreign-parent"),
                commit("foreign-parent", "other", 1L));
        ResumeVersionControlService service = versions(mongo);
        assertThrows(BusinessException.class, () -> service.getRecentCommits("mine", 1L, 10));
        assertThrows(BusinessException.class, () -> service.revertCommit("mine", 1L, "owned-head", null));
        assertThrows(BusinessException.class, () -> service.switchBranch("mine", 1L, "foreign", null));
        assertThrows(BusinessException.class, () -> service.mergeBranch("mine", 1L, "foreign", "main", null));
        meta.setHeadCommit("foreign-parent");
        assertThrows(BusinessException.class, () -> service.commit("mine", 1L, "save", "new", null));
        assertThrows(BusinessException.class, () -> service.getCommitDebugInfo("mine", 1L, "owned-head"));
        verify(mongo, never()).updateFirst(any(Query.class), any(Update.class), eq(Resume.class));
        verify(mongo, never()).insert(any(Resume.class));
    }

    @Test
    void legitimateHistoryAndCheckoutStillWork() {
        Resume meta = new Resume(); meta.setHeadCommit("head");
        MongoTemplate mongo = fixture(meta, commit("head", "mine", 1L, "initial"), commit("initial", "mine", 1L));
        ResumeVersionControlService service = versions(mongo);
        assertEquals(List.of("head", "initial"), service.getRecentCommits("mine", 1L, 10).stream().map(Resume::getId).toList());
        service.checkout("mine", 1L, "initial", null);
        assertEquals("secret initial", meta.getContent());
        assertEquals(1, meta.getWorkspaceRevision());
    }
}
