package com.david.hlp.commons.recruitment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobAnalysisReaderTest {
    @Test
    void arraysPreserveExactTextAndAmbiguousLegacyStaysRaw() throws Exception {
        var values = List.of("Java, Python", " [A] and (B) ", "say \"hi\"", "emoji 😀\nnext");
        assertEquals(values, JobArrays.read(new ObjectMapper().writeValueAsString(values)));
        for (String legacy : List.of("[Java, Python]", "[\"broken]", "[null]", "[42]", "[] trailing")) {
            assertFalse(JobArrays.isJsonArray(legacy));
            assertEquals(List.of(legacy), JobArrays.read(legacy));
        }
        assertEquals(List.of(), JobArrays.read("[]"));
        assertEquals(List.of(), JobArrays.read(null));
    }

    @Test
    void onlyCurrentSuccessfulJsonProjectionIsCompleted() {
        String source = "{\"jobDescription\":{\"a\":1,\"b\":\"😀\"}}";
        assertEquals("complete", status(source, "{\"jobDescription\":{\"b\":\"😀\",\"a\":1}}", "completed", "complete", "[\"Java, Python\"]"));
        assertEquals("no_evidence", status(source, source, "completed", "no_evidence", "[]"));
        assertEquals("stale", status(source, "{}", "completed", "complete", "[]"));
        assertEquals("needs_review", status(source, source, "completed", "complete", "[legacy]"));
        assertEquals("needs_review", status(source, source, "completed", "no_evidence", "[\"old\"]"));
        assertEquals("needs_review", status(source, source, "completed", "bad", "[]"));
        for (String state : List.of("pending", "running", "needs_review", "failed")) {
            assertEquals(state, status(source, source, state, "complete", "[legacy]"));
        }
        assertEquals("pending", status(source, source, "retry_wait", "complete", "[]"));
        assertNull(status(source, source, "future_state", "complete", "[]"));
        assertEquals("unparsed", JobAnalysisReader.status(source, null, null, null, null, null));
        assertEquals("legacy_unverified", JobAnalysisReader.status(source, null, null, null, "[old]", null));
    }

    @Test
    void factualIdentityAndFreshnessIgnoreOnlyMetadata() throws Exception {
        var json = new ObjectMapper();
        String a = "{\"jobTags\":[\"😀Java\",\"Python\"],\"jobDescription\":{\"b\":\"B\",\"a\":\"A\"}}";
        String b = "{\"company\":\"different\",\"updateTime\":2,\"analysisReport\":{\"text\":\"ignore\"},\"jobDescription\":{\"a\":\"A\",\"b\":\"B\"},\"jobTags\":[\"😀Java\",\"Python\"]}";
        assertEquals(JobAnalysisFacts.digest(json.readTree(a)), JobAnalysisFacts.digest(json.readTree(b)));
        assertEquals("complete", status(b, a, "completed", "complete", "[]"));
        for (String changed : List.of(a.replace("Python", "Rust"), a.replace("😀Java", "Java"),
                a.replace("[\"😀Java\",\"Python\"]", "[\"Python\",\"😀Java\"]"))) {
            assertNotEquals(JobAnalysisFacts.digest(json.readTree(a)), JobAnalysisFacts.digest(json.readTree(changed)));
            assertEquals("stale", status(changed, a, "completed", "complete", "[]"));
        }
        assertEquals(JobAnalysisFacts.digest(json.readTree("{}")), JobAnalysisFacts.digest(json.readTree("{\"jobTags\":null}")));
        assertNotEquals(JobAnalysisFacts.digest(json.readTree("{}")), JobAnalysisFacts.digest(json.readTree("{\"jobTags\":[]}")));
        assertNotEquals(JobAnalysisFacts.digest(json.readTree("{\"jobTags\":[]}")), JobAnalysisFacts.digest(json.readTree("{\"jobTags\":[\"\"]}")));
    }

    private String status(String detail, String source, String state, String status, String requirements) {
        return JobAnalysisReader.status(detail, source, state, "{\"status\":\"" + status + "\"}", requirements, "[]");
    }

    @Test
    void schemaCompatibilityNeverSwallowsSqlFailuresAndKeepsRawDeletionFilter() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.getMetaData()).thenThrow(new SQLException("synthetic"));
        assertThrows(SQLException.class, () -> JobAnalysisReader.table(connection));
        assertTrue(JobAnalysisReader.table(true).contains("a.job_id = d.id"));
        assertTrue(JobAnalysisReader.table(true).contains("newer.updated_at = a.updated_at AND newer.id > a.id"));
        for (boolean sidecar : List.of(true, false)) {
            assertTrue(JobAnalysisReader.table(sidecar).contains("WHERE d.is_deleted = 0"));
        }
        assertFalse(JobAnalysisReader.table(false).contains("JOIN job_ai_analysis"));
        Connection metadataConnection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        ResultSet tables = mock(ResultSet.class);
        when(metadataConnection.getMetaData()).thenReturn(metadata);
        when(metadata.getTables(null, null, "%", new String[]{"TABLE"})).thenReturn(tables);
        when(tables.next()).thenReturn(false);
        assertEquals(JobAnalysisReader.table(false), JobAnalysisReader.table(metadataConnection));
        when(tables.next()).thenReturn(true, false);
        when(tables.getString("TABLE_NAME")).thenReturn("job_ai_analysis");
        assertEquals(JobAnalysisReader.table(true), JobAnalysisReader.table(metadataConnection));
    }
}
