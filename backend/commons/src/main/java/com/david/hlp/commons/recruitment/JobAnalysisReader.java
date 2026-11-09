package com.david.hlp.commons.recruitment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.sql.Connection;
import java.sql.SQLException;

public final class JobAnalysisReader {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private JobAnalysisReader() {}

    public static String status(String detail, String source, String state, String result,
            String requirements, String benefits) {
        if (state == null) {
            return JobArrays.read(requirements).isEmpty() && JobArrays.read(benefits).isEmpty()
                    ? "unparsed" : "legacy_unverified";
        }
        try {
            var current = detail == null ? null : JSON.readTree(detail);
            var snapshot = source == null ? null : JSON.readTree(source);
            if (!JobAnalysisFacts.same(current, snapshot)) return "stale";
            switch (state) {
                case "completed":
                    var output = result == null ? null : JSON.readTree(result);
                    String status = output == null ? "" : output.path("status").asText();
                    if (!("complete".equals(status) || "no_evidence".equals(status))) return "needs_review";
                    if (!JobArrays.isJsonArray(requirements) || !JobArrays.isJsonArray(benefits)) return "needs_review";
                    if ("no_evidence".equals(status)
                            && (!JobArrays.read(requirements).isEmpty() || !JobArrays.read(benefits).isEmpty())) return "needs_review";
                    return status;
                case "pending": case "running": case "needs_review": case "failed": return state;
                case "retry_wait": return "pending";
                case "superseded": return "stale";
                default: return null;
            }
        } catch (Exception e) {
            return "needs_review";
        }
    }

    /** Metadata absence is supported; SQL/auth/schema failures must propagate. */
    public static String table(Connection connection) throws SQLException {
        boolean exists = false;
        try (var tables = connection.getMetaData().getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                if ("job_ai_analysis".equalsIgnoreCase(tables.getString("TABLE_NAME"))) exists = true;
            }
        }
        return table(exists);
    }

    public static String table(boolean sidecarExists) {
        String columns = sidecarExists
                ? "a.state AS analysis_state, a.source_json AS analysis_source, a.result_json AS analysis_result, "
                    + "a.model_id AS analysis_model, a.pipeline_version AS analysis_version"
                : "CAST(NULL AS CHAR) AS analysis_state, CAST(NULL AS CHAR) AS analysis_source, "
                    + "CAST(NULL AS CHAR) AS analysis_result, CAST(NULL AS CHAR) AS analysis_model, CAST(NULL AS CHAR) AS analysis_version";
        String join = sidecarExists ? " LEFT JOIN job_ai_analysis a ON a.job_id = d.id AND NOT EXISTS "
                + "(SELECT 1 FROM job_ai_analysis newer WHERE newer.job_id = a.job_id AND "
                + "(newer.updated_at > a.updated_at OR (newer.updated_at = a.updated_at AND newer.id > a.id)))" : "";
        return "(SELECT d.*, " + columns + " FROM t_job_detail d" + join
                + " WHERE d.is_deleted = 0) recruitment_analysis";
    }
}
