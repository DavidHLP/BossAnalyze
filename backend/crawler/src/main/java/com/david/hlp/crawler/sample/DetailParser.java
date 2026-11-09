package com.david.hlp.crawler.sample;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Whitelist job/company facts; never serialize the response or recruiter nodes. */
public final class DetailParser {
    static final ObjectMapper JSON = new ObjectMapper();
    private DetailParser() { }

    static Map<String, Object> base(SourceUrl source, String fetchedAt) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schemaVersion", 1);
        result.put("extractorVersion", "boss-browser-v1");
        result.put("platform", "BOSS");
        result.put("sourceUrl", source.uri().toString());
        result.put("sourceJobId", source.jobId());
        result.put("fetchedAt", fetchedAt);
        result.put("identity", hash("BOSS:" + source.uri()));
        return result;
    }

    static Map<String, Object> parse(SourceUrl source, String body, String fetchedAt) {
        Map<String, Object> result = base(source, fetchedAt);
        JsonNode root;
        try { root = JSON.readTree(body); }
        catch (Exception exception) { return failed(result, "invalid_detail_json"); }
        if (root == null || !root.path("code").isIntegralNumber() || !root.path("code").canConvertToInt() || root.path("code").asInt() != 0)
            return failed(result, "api_error");
        JsonNode job = root.path("zpData").path("jobInfo");
        String id = scalar(job.get("encryptId"));
        if (source.jobId() == null || !source.jobId().equals(id)) return failed(result, "identity_mismatch");
        String title = scalar(job.get("jobName"));
        String jd = scalar(job.get("postDescription"));
        if (title == null || jd == null) return failed(result, "missing_detail");
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("title", title);
        facts.put("company", scalar(root.path("zpData").path("brandComInfo").get("brandName")));
        facts.put("city", scalar(job.get("locationName")));
        facts.put("salary", scalar(job.get("salaryDesc")));
        facts.put("experience", scalar(job.get("experienceName")));
        facts.put("education", scalar(job.get("degreeName")));
        facts.put("rawJd", jd);
        facts.put("rawBenefits", null); // No verified benefits field; skills are not welfare.
        result.put("facts", facts);
        Map<String, Object> basic = new LinkedHashMap<>();
        basic.put("positionName", title);
        basic.put("salary", facts.get("salary"));
        basic.put("city", facts.get("city"));
        basic.put("experience", facts.get("experience"));
        basic.put("degree", facts.get("education"));
        basic.put("address", null);
        Map<String, Object> description = new LinkedHashMap<>();
        description.put("fullDescription", jd);
        description.put("requirements", null);
        description.put("responsibilities", null);
        description.put("keywords", null);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("basicInfo", basic);
        detail.put("jobDescription", description);
        detail.put("jobTags", null);
        result.put("detailData", detail);
        result.put("revision", hash(toJson(facts)));
        result.put("splitGroup", result.get("identity"));
        result.put("split", "blind_holdout");
        result.put("samplingSource", "boss_browser_detail_response");
        result.put("detailComplete", true);
        result.put("status", "ok");
        return result;
    }

    static Map<String, Object> failed(Map<String, Object> result, String status) {
        result.put("status", status);
        result.put("detailComplete", false);
        return result;
    }

    static Map<String, Object> labels(Map<String, Object> sample) {
        Map<String, Object> labels = new LinkedHashMap<>();
        for (String key : List.of("identity", "revision", "splitGroup", "split")) labels.put(key, sample.get(key));
        labels.put("status", "pending");
        for (String key : List.of("requirements", "benefits", "evidenceSpans", "anonymizedResume", "jobGroup",
                "ranking", "relevance", "annotator", "annotationVersion", "adjudication")) labels.put(key, null);
        return labels;
    }

    static String scalar(JsonNode node) {
        return node != null && node.isTextual() && !node.textValue().isBlank() ? node.textValue() : null;
    }
    static String toJson(Object value) {
        try { return JSON.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalArgumentException("serialization_failed"); }
    }
    static String hash(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
}
