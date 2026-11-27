package com.david.hlp.web.resume;

import com.david.hlp.commons.utils.RedisLockUtil;
import com.david.hlp.web.common.exception.BusinessException;
import com.david.hlp.web.resume.entity.Resume;
import com.david.hlp.web.resume.exception.ResumeWorkspaceConflictException;
import com.david.hlp.web.resume.service.ResumeService;
import com.david.hlp.web.resume.service.ResumeVersionControlService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfSystemProperty(named = "bossanalyze.resume.integration", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResumeWorkspaceConcurrencyTest {
    private static final long OWNER = 1L;
    private MongoClient mongoClient;
    private RedissonClient redis;
    private MongoTemplate mongo;
    private RedisLockUtil lock;
    private ResumeVersionControlService versions;
    private ResumeService resumes;

    @BeforeAll
    void connectToLocalServices() {
        mongoClient = MongoClients.create("mongodb://127.0.0.1:27017/?serverSelectionTimeoutMS=3000");
        mongo = new MongoTemplate(mongoClient, "resume_workspace_test_" + UUID.randomUUID().toString().replace("-", ""));
        Config config = new Config().setThreads(2).setNettyThreads(2);
        config.useSingleServer().setAddress(System.getProperty("bossanalyze.resume.test.redis", "redis://127.0.0.1:6379"))
                .setConnectionMinimumIdleSize(1).setSubscriptionConnectionMinimumIdleSize(1);
        redis = Redisson.create(config);
        lock = new RedisLockUtil(redis);
        versions = new ResumeVersionControlService(mongo, lock);
        resumes = new ResumeService(mongo, versions);
    }

    @AfterAll
    void closeIsolatedFixtures() {
        try {
            if (mongo != null) {
                mongo.getDb().drop();
            }
        } finally {
            try {
                if (redis != null) {
                    redis.shutdown();
                }
            } finally {
                if (mongoClient != null) {
                    mongoClient.close();
                }
            }
        }
    }

    @Test
    void twoSavesAtLegacyRevisionZeroPublishOneSnapshot() throws Exception {
        Resume resume = createResume();
        String id = resume.getId();
        // A MongoDB record written before this change has no workspace_revision field.
        mongo.getCollection("resumes").updateOne(new Document("_id", new ObjectId(id)),
                new Document("$unset", new Document("workspace_revision", "")));
        assertEquals(0, getResume(id).getWorkspaceRevision());

        List<Outcome> outcomes = race(() -> versions.commit(id, OWNER, "first", "first", 0L),
                () -> versions.commit(id, OWNER, "second", "second", 0L));
        Resume current = getResume(id);
        assertOneWinner(outcomes, current, 1);
        assertEquals(List.of(current.getHeadCommit(), resume.getHeadCommit()), ids(versions.getRecentCommits(id, OWNER, 10)));
        assertEquals(2, versions.getCommitHistory(id, OWNER).size());
        assertTrue(versions.getCommitHistory(id, OWNER).stream().allMatch(snapshot -> "commit".equals(snapshot.getDocType())));
        assertEquals(409, assertThrows(BusinessException.class, () -> resumes.deleteResume(id, OWNER, 0L)).getCode());

        String legacySave = versions.commit(id, OWNER, "legacy caller", "legacy", null);
        assertEquals(legacySave, getResume(id).getHeadCommit());
        assertEquals(2, getResume(id).getWorkspaceRevision());
    }

    @Test
    void saveAndStashPopAtSameRevisionHaveOneWinner() throws Exception {
        Resume resume = createResume();
        String id = resume.getId();
        versions.stash(id, OWNER, "stash", 0L);

        List<Outcome> outcomes = race(() -> versions.commit(id, OWNER, "save", "saved", 1L),
                () -> versions.stashPop(id, OWNER, 1L));
        Resume current = getResume(id);
        assertOneWinner(outcomes, current, 2);
        if (outcomes.get(0).conflict() == null) {
            assertEquals("saved", current.getContent());
            assertEquals(1, current.getStashStack().size());
            assertEquals(2, versions.getCommitHistory(id, OWNER).size());
        } else {
            assertEquals("stash", current.getContent());
            assertTrue(current.getStashStack().isEmpty());
            assertEquals(List.of(resume.getHeadCommit()), ids(versions.getCommitHistory(id, OWNER)));
        }
    }

    @Test
    void stagedSaveLosingItsLockCannotPublishOrBecomeAParent() throws Exception {
        Resume resume = createResume();
        String id = resume.getId();
        AtomicReference<String> staged = new AtomicReference<>();
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch advance = new CountDownLatch(1);
        var writer = pausedWriter(staged, inserted, advance);
        var executor = Executors.newSingleThreadExecutor();
        try {
            Future<Outcome> loser = executor.submit(() -> attempt(() -> writer.commit(id, OWNER, "loser", "loser", 0L)));
            assertTrue(inserted.await(5, TimeUnit.SECONDS));
            assertHidden(id, staged.get(), resume.getHeadCommit());
            String winner = versions.commit(id, OWNER, "winner", "winner", 0L);
            advance.countDown();
            assertEquals(new ResumeWorkspaceConflictException.State(1, winner), loser.get(5, TimeUnit.SECONDS).conflict());
            versions.stash(id, OWNER, "next write finalizes winner", 1L);
            assertHidden(id, staged.get(), winner);
            assertEquals(List.of(winner, resume.getHeadCommit()), ids(versions.getRecentCommits(id, OWNER, 10)));
        } finally {
            advance.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void deletionWinningAfterSnapshotInsertDoesNotResurrectWorkspace() throws Exception {
        Resume resume = createResume();
        String id = resume.getId();
        AtomicReference<String> staged = new AtomicReference<>();
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch advance = new CountDownLatch(1);
        var writer = pausedWriter(staged, inserted, advance);
        var executor = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> loser = executor.submit(() -> assertThrows(BusinessException.class,
                    () -> writer.commit(id, OWNER, "loser", "loser", 0L)).getCode());
            assertTrue(inserted.await(5, TimeUnit.SECONDS));
            assertHidden(id, staged.get(), resume.getHeadCommit());
            resumes.deleteResume(id, OWNER, 0L);
            advance.countDown();
            assertEquals(404, loser.get(5, TimeUnit.SECONDS));
            assertNull(getResume(id));
            assertEquals(404, assertThrows(BusinessException.class, () -> versions.getCommitHistory(id, OWNER)).getCode());
            assertEquals(404, assertThrows(BusinessException.class,
                    () -> versions.getCommitDebugInfo(id, OWNER, staged.get())).getCode());
        } finally {
            advance.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void failedMetadataUpdateLeavesStagedSnapshotInvisibleAcrossRetries() {
        Resume resume = createResume();
        String id = resume.getId();
        MongoTemplate failingMongo = spy(mongo);
        doThrow(new DataAccessResourceFailureException("interrupted before pointer update")).when(failingMongo)
                .updateFirst(any(Query.class), argThat((UpdateDefinition update) ->
                        ((Document) update.getUpdateObject().get("$set")).containsKey("workspaceRevision")), eq(Resume.class));
        var writer = new ResumeVersionControlService(failingMongo, lock);
        assertThrows(DataAccessResourceFailureException.class, () -> writer.commit(id, OWNER, "failed", "failed", 0L));
        assertEquals(0, getResume(id).getWorkspaceRevision());
        String failed = mongo.getCollection("resumes").find(new Document("resume_id", id).append("message", "failed"))
                .first().getString("_id");
        assertHidden(id, failed, resume.getHeadCommit());
        String winner = versions.commit(id, OWNER, "retry", "saved", 0L);
        assertHidden(id, failed, winner);
        assertEquals(2, versions.getCommitHistory(id, OWNER).size());
    }

    @Test
    void unavailableLockAndForeignOwnerCannotChangeWorkspace() {
        Resume resume = createResume();
        String id = resume.getId();
        var blocked = new ResumeVersionControlService(mongo, mock(RedisLockUtil.class));
        assertEquals(503, assertThrows(BusinessException.class,
                () -> blocked.commit(id, OWNER, "save", "blocked", 0L)).getCode());
        assertEquals(404, assertThrows(BusinessException.class,
                () -> versions.commit(id, 2L, "save", "foreign", 9L)).getCode());
        assertEquals(404, assertThrows(BusinessException.class,
                () -> resumes.deleteResume(id, 2L, 0L)).getCode());
        assertEquals(0, getResume(id).getWorkspaceRevision());
        assertEquals(List.of(resume.getHeadCommit()), ids(versions.getCommitHistory(id, OWNER)));
    }

    @Test
    void branchOperationsCheckoutResetRevertAndRepairAdvanceRevision() {
        Resume resume = createResume();
        String id = resume.getId();
        versions.createBranch(id, OWNER, "feature", 0L);
        versions.switchBranch(id, OWNER, "feature", 1L);
        String feature = versions.commit(id, OWNER, "feature changes", "feature", 2L);
        String merged = versions.mergeBranch(id, OWNER, "feature", "main", 3L);
        Resume afterMerge = getResume(id);
        assertEquals(feature, afterMerge.getHeadCommit());
        assertEquals(merged, afterMerge.getBranches().get("main"));
        assertTrue(ids(versions.getCommitHistory(id, OWNER)).contains(merged));
        versions.checkout(id, OWNER, resume.getHeadCommit(), 4L);
        versions.resetToCommit(id, OWNER, resume.getHeadCommit(), 5L);
        String reverted = versions.revertCommit(id, OWNER, feature, 6L);
        assertEquals(true, versions.fixDuplicateCommitIds(id, OWNER, 7L).get("success"));
        assertEquals(8, getResume(id).getWorkspaceRevision());
        assertEquals(reverted, getResume(id).getHeadCommit());
        assertEquals("initial", getResume(id).getContent());
    }

    private Resume createResume() {
        Resume request = new Resume();
        request.setTitle("Synthetic resume");
        request.setContent("initial");
        return resumes.createResume(request, OWNER);
    }

    private Resume getResume(String id) {
        return resumes.getResumeById(id, OWNER);
    }

    private ResumeVersionControlService pausedWriter(AtomicReference<String> staged, CountDownLatch inserted,
            CountDownLatch advance) {
        MongoTemplate delayedMongo = spy(mongo);
        doAnswer(call -> {
            Resume snapshot = (Resume) call.callRealMethod();
            staged.set(snapshot.getId());
            inserted.countDown();
            assertTrue(advance.await(5, TimeUnit.SECONDS));
            return snapshot;
        }).when(delayedMongo).insert(any(Resume.class));
        // The paused client continues after losing its lease. A normal client still uses Redis.
        RedisLockUtil lostLock = mock(RedisLockUtil.class);
        when(lostLock.tryLock(anyString(), anyLong(), anyLong(), any())).thenReturn(true);
        return new ResumeVersionControlService(delayedMongo, lostLock);
    }

    private void assertHidden(String id, String staged, String head) {
        assertFalse(ids(versions.getCommitHistory(id, OWNER)).contains(staged));
        assertFalse(ids(versions.getRecentCommits(id, OWNER, 10)).contains(staged));
        assertEquals(404, assertThrows(BusinessException.class,
                () -> versions.getCommitDebugInfo(id, OWNER, staged)).getCode());
        assertEquals(404, assertThrows(BusinessException.class,
                () -> versions.getCommitsBetween(id, OWNER, head, staged)).getCode());
        assertEquals(404, assertThrows(BusinessException.class,
                () -> versions.checkout(id, OWNER, staged, null)).getCode());
    }

    private static List<String> ids(List<Resume> history) {
        return history.stream().map(Resume::getId).toList();
    }

    private static List<Outcome> race(Callable<String> first, Callable<String> second) throws Exception {
        CyclicBarrier ready = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<Outcome> left = executor.submit(() -> {
                ready.await(5, TimeUnit.SECONDS);
                return attempt(first);
            });
            Future<Outcome> right = executor.submit(() -> {
                ready.await(5, TimeUnit.SECONDS);
                return attempt(second);
            });
            return List.of(left.get(10, TimeUnit.SECONDS), right.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private static Outcome attempt(Callable<String> write) throws Exception {
        try {
            return new Outcome(write.call(), null);
        } catch (ResumeWorkspaceConflictException e) {
            assertEquals(409, e.getCode());
            return new Outcome(null, e.getState());
        }
    }

    private static void assertOneWinner(List<Outcome> outcomes, Resume current, long revision) {
        assertEquals(revision, current.getWorkspaceRevision());
        assertEquals(1, outcomes.stream().filter(outcome -> outcome.result() != null).count());
        assertEquals(1, outcomes.stream().filter(outcome -> outcome.conflict() != null).count());
        assertTrue(outcomes.stream().anyMatch(outcome -> new ResumeWorkspaceConflictException.State(
                revision, current.getHeadCommit()).equals(outcome.conflict())));
    }

    private record Outcome(String result, ResumeWorkspaceConflictException.State conflict) {}
}
