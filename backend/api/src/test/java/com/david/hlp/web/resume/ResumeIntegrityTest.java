package com.david.hlp.web.resume;

import com.david.hlp.web.resume.entity.Resume;
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
        ResumeVersionControlService versions = mock(ResumeVersionControlService.class);
        ResumeService service = new ResumeService(mongo, versions);
        Resume request = new Resume();
        request.setId("victim");
        request.setUserId(99L);
        request.setDocType("commit");
        request.setTitle("My resume");
        request.setContent("hello");
        request.setResumeId("victim-resume");
        request.setHeadCommit("foreign");
        request.getBranches().put("evil", "foreign");
        request.getMetadata().put("internal", "forged");
        when(mongo.insert(any(Resume.class))).thenAnswer(call -> {
            Resume inserted = call.getArgument(0);
            assertNull(inserted.getId());
            assertEquals(1L, inserted.getUserId());
            assertEquals("meta", inserted.getDocType());
            assertNull(inserted.getResumeId());
            assertNull(inserted.getHeadCommit());
            assertTrue(inserted.getBranches().isEmpty());
            assertTrue(inserted.getMetadata().isEmpty());
            inserted.setId("fresh");
            return inserted;
        });
        when(versions.createCommit(eq("fresh"), eq(1L), anyString(), eq("hello"), eq("main"), anyList(), eq("normal")))
                .thenReturn("initial");
        when(mongo.save(any(Resume.class))).thenAnswer(call -> call.getArgument(0));
        Resume created = service.createResume(request, 1L);
        assertEquals("fresh", created.getId());
        assertEquals("My resume", created.getTitle());
        assertEquals(Map.of("main", "initial"), created.getBranches());
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
        assertSame(created, service.getResumeById("fresh", 1L));
        assertNull(service.getResumeById("fresh", 2L));
    }

    @Test
    void titleOnlySaveKeepsContentAndUpdatesOwnedMeta() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        ResumeVersionControlService versions = mock(ResumeVersionControlService.class);
        Resume existing = new Resume();
        existing.setContent("keep content");
        when(mongo.findOne(any(Query.class), eq(Resume.class))).thenReturn(existing);
        Resume request = new Resume();
        request.setTitle("renamed");
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(Resume.class))).thenAnswer(call -> {
            existing.setTitle((String) ((Document) ((Update) call.getArgument(1)).getUpdateObject().get("$set")).get("title"));
            return null;
        });
        ResumeService service = new ResumeService(mongo, versions);
        assertEquals("renamed", service.updateResume("mine", request, 1L).getTitle());
        assertEquals("renamed", service.getResumeById("mine", 1L).getTitle());
        verify(versions).commit("mine", 1L, "Updated via basic save", "keep content");
        verify(mongo).updateFirst(argThat((Query q) -> q.getQueryObject().equals(
                new Document("id", "mine").append("userId", 1L).append("docType", "meta"))),
                argThat((Update u) -> "renamed".equals(((Document) u.getUpdateObject().get("$set")).get("title"))), eq(Resume.class));
    }

    private MongoTemplate fixture(Resume meta, Resume... commits) {
        MongoTemplate mongo = mock(MongoTemplate.class);
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
            ResumeVersionControlService service = new ResumeVersionControlService(mongo);
            assertThrows(BusinessException.class, () -> service.checkout("mine", 1L, "foreign"));
            assertThrows(BusinessException.class, () -> service.resetToCommit("mine", 1L, "foreign"));
            assertThrows(BusinessException.class, () -> service.revertCommit("mine", 1L, "foreign"));
            assertThrows(BusinessException.class, () -> service.getCommitsBetween("mine", 1L, "foreign", "foreign"));
            assertThrows(BusinessException.class, () -> service.getCommitDebugInfo("mine", 1L, "foreign"));
            assertEquals("original", meta.getContent());
            assertEquals("head", meta.getHeadCommit());
            verify(mongo, never()).save(any(Resume.class));
            verify(mongo, never()).insert(any(Resume.class));
        }
    }

    @Test
    void recursiveParentsAndStoredBranchPointersRemainScoped() {
        Resume meta = new Resume(); meta.setHeadCommit("owned-head"); meta.setContent("original");
        meta.getBranches().put("main", "owned-head"); meta.getBranches().put("foreign", "foreign-parent");
        MongoTemplate mongo = fixture(meta, commit("owned-head", "mine", 1L, "foreign-parent"),
                commit("foreign-parent", "other", 1L));
        ResumeVersionControlService service = new ResumeVersionControlService(mongo);
        assertThrows(BusinessException.class, () -> service.getRecentCommits("mine", 1L, 10));
        assertThrows(BusinessException.class, () -> service.revertCommit("mine", 1L, "owned-head"));
        assertThrows(BusinessException.class, () -> service.switchBranch("mine", 1L, "foreign"));
        assertThrows(BusinessException.class, () -> service.mergeBranch("mine", 1L, "foreign", "main"));
        meta.setHeadCommit("foreign-parent");
        assertThrows(BusinessException.class, () -> service.commit("mine", 1L, "save", "new"));
        assertThrows(BusinessException.class, () -> service.getCommitDebugInfo("mine", 1L, "owned-head"));
        verify(mongo, never()).save(any(Resume.class));
        verify(mongo, never()).insert(any(Resume.class));
    }

    @Test
    void legitimateHistoryAndCheckoutStillWork() {
        Resume meta = new Resume(); meta.setHeadCommit("head");
        MongoTemplate mongo = fixture(meta, commit("head", "mine", 1L, "initial"), commit("initial", "mine", 1L));
        ResumeVersionControlService service = new ResumeVersionControlService(mongo);
        assertEquals(List.of("head", "initial"), service.getRecentCommits("mine", 1L, 10).stream().map(Resume::getId).toList());
        service.checkout("mine", 1L, "initial");
        assertEquals("secret initial", meta.getContent());
        verify(mongo).save(meta);
    }
}
