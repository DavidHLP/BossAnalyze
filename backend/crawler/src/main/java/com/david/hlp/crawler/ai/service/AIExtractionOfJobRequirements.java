package com.david.hlp.crawler.ai.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import java.math.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

/** Business-only Python client. No model retries or user credentials cross this boundary. */
@Service
public class AIExtractionOfJobRequirements {
    private final ObjectMapper json;
    private RestTemplate http;
    private final String url;
    private final String token;
    private final String model;
    @org.springframework.beans.factory.annotation.Autowired
    public AIExtractionOfJobRequirements(ObjectMapper json,
            @Value("${crawler.ai.service-url:http://127.0.0.1:8090}") String url,
            @Value("${AI_SERVICE_TOKEN:}") String token,
            @Value("${JEV_MODEL:typesafe/jev-1.14.0}") String model) {
        this.json = json; this.url = url; this.token = token; this.model = model;
    }
    AIExtractionOfJobRequirements(ObjectMapper json, String url, String token, String model, RestTemplate http) {
        this(json,url,token,model); this.http=http;
    }
    public String model() { return model; }
    public JsonNode detail(String source) {
        try { JsonNode n = json.readTree(source); if (n != null && n.isObject()) return n; }
        catch (Exception ignored) { }
        throw error(422, "AI_INVALID_INPUT");
    }
    private JsonNode post(String path, ObjectNode body, int timeoutMs) {
        if (token == null || token.isBlank()) throw error(503, "AI_UNCONFIGURED");
        if (timeoutMs < 1) throw error(504, "AI_DEADLINE");
        body.put("timeoutMs", Math.min(timeoutMs, 30000));
        if (body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 262144)
            throw error(422, "AI_INPUT_LIMIT");
        var headers = new HttpHeaders(); headers.setBearerAuth(token); headers.setContentType(MediaType.APPLICATION_JSON);
        RestTemplate transport=http;
        if (transport==null) {
            var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();
            int connect=Math.min(1000,Math.max(1,timeoutMs/2));
            factory.setConnectTimeout(connect);factory.setReadTimeout(Math.max(1,timeoutMs-connect));
            transport=new RestTemplate(factory);
        }
        try {
            JsonNode result = transport.postForObject(url + path, new HttpEntity<>(body, headers), JsonNode.class);
            if (result == null || !result.isObject() || !result.path("model").asText().equals(model)
                    || !result.path("policyVersion").asText().equals("jev-policy-1")
                    || !result.path("requestId").isTextual() || !result.path("usage").isObject()) throw error(502, "AI_BAD_OUTPUT");
            for (String field : List.of("inputTokens", "outputTokens")) {
                JsonNode n = result.path("usage").path(field);
                if (!(n.isNull() || n.isIntegralNumber() && n.canConvertToLong() && n.asLong() >= 0)) throw error(502, "AI_BAD_OUTPUT");
            }
            return result;
        } catch (RestClientResponseException e) {
            throw error(e.getStatusCode().value() == 504 ? 504 : e.getStatusCode().value() == 502 ? 502 : 503, "AI_UNAVAILABLE");
        } catch (ResourceAccessException e) { throw error(504, "AI_DEADLINE"); }
        catch (RestClientException e) { throw error(502, "AI_BAD_OUTPUT"); }
    }
    public JsonNode analyze(String id, String source) {
        JsonNode input = detail(source);
        var body = json.createObjectNode().put("jobId", id); body.set("detailData", input);
        JsonNode result = post("/internal/v1/job-analysis", body, 30000);
        if (!id.equals(result.path("jobId").asText()) || !result.path("pipelineVersion").asText().equals("jev-1")
                || !Set.of("complete", "no_evidence", "needs_review").contains(result.path("status").asText())) throw error(502, "AI_BAD_OUTPUT");
        for (String field : List.of("requirements", "benefits", "coreRequirements")) {
            JsonNode items = result.path(field);
            if (!items.isArray() || items.size() > (field.equals("coreRequirements") ? 20 : 48)) throw error(502, "AI_BAD_OUTPUT");
            Set<String> ids = new HashSet<>();
            for (JsonNode item : items) {
                if (!item.path("candidateId").isTextual() || !ids.add(item.path("candidateId").asText()) || !item.path("text").isTextual()) throw error(502, "AI_BAD_OUTPUT");
                optionalNumber(item.path("probability"), 1); optionalNumber(item.path("importance"), 4);
                evidence(item.path("evidence"), input, item.path("text").asText());
            }
        }
        if (result.path("status").asText().equals("no_evidence") && (result.path("requirements").size() + result.path("benefits").size() + result.path("coreRequirements").size() != 0)) throw error(502,"AI_BAD_OUTPUT");
        return result;
    }
    static void optionalNumber(JsonNode n, int max) {
        if (n.isNull()) return;
        if (!n.isNumber() || !Double.isFinite(n.asDouble()) || n.asDouble() < 0 || n.asDouble() > max) throw error(502, "AI_BAD_OUTPUT");
    }
    static void evidence(JsonNode evidence, JsonNode source, String text) {
        if (!evidence.isArray() || evidence.isEmpty()) throw error(502, "AI_BAD_EVIDENCE");
        boolean matches = text == null;
        for (JsonNode e : evidence) {
            try {
                String path = e.path("sourcePath").textValue();
                if (path == null || !path.startsWith("/")) throw new IllegalArgumentException();
                JsonNode value = source.at(path);
                if (!value.isTextual() || !e.path("start").isIntegralNumber() || !e.path("end").isIntegralNumber()) throw new IllegalArgumentException();
                String s = value.asText(); int start = e.path("start").intValue(), end = e.path("end").intValue();
                if (start < 0 || end <= start || end > s.codePointCount(0,s.length())) throw new IllegalArgumentException();
                String slice = s.substring(s.offsetByCodePoints(0,start), s.offsetByCodePoints(0,end));
                if (!slice.equals(e.path("text").textValue())) throw new IllegalArgumentException();
                matches |= slice.equals(text);
            } catch (Exception ex) { throw error(502,"AI_BAD_EVIDENCE"); }
        }
        if (!matches) throw error(502,"AI_BAD_EVIDENCE");
    }
    public JsonNode match(String resume, List<ObjectNode> jobs, int timeoutMs) {
        var body = json.createObjectNode().put("resume", resume); body.set("jobs", json.valueToTree(jobs));
        JsonNode result = post("/internal/v1/match-batch", body, timeoutMs);
        if (!result.path("items").isArray() || !result.path("complete").isBoolean() || result.path("items").size() != jobs.size()) throw error(502,"AI_BAD_OUTPUT");
        Map<String,JsonNode> inputs = new HashMap<>(); for (var job:jobs) inputs.put(job.path("jobId").asText(),job.path("detailData"));
        Set<String> seen = new HashSet<>(); boolean complete = true;
        for (JsonNode item : result.path("items")) {
            String id = item.path("jobId").asText(), status = item.path("status").asText();
            if (!item.path("jobId").isTextual() || !inputs.containsKey(id) || !seen.add(id) || !Set.of("scored","insufficient_evidence","failed").contains(status)) throw error(502,"AI_BAD_OUTPUT");
            optionalNumber(item.path("score"),10);
            if (status.equals("scored") != item.path("score").isNumber()) throw error(502,"AI_BAD_OUTPUT");
            complete &= status.equals("scored");
            Set<String> names = new HashSet<>();
            JsonNode dims = item.path("dimensions"); if (!dims.isArray()) throw error(502,"AI_BAD_OUTPUT");
            for (JsonNode d : dims) {
                if (!Set.of("skills","relevant_experience","experience","education").contains(d.path("name").asText()) || !names.add(d.path("name").asText()) || !Set.of("scored","unknown","not_applicable").contains(d.path("status").asText())) throw error(502,"AI_BAD_OUTPUT");
                optionalNumber(d.path("rawScore"),4); optionalNumber(d.path("confidence"),1);
                if (d.path("status").asText().equals("scored") != d.path("rawScore").isNumber() || status.equals("scored") && d.path("status").asText().equals("unknown")) throw error(502,"AI_BAD_OUTPUT");
                JsonNode probs = d.path("probabilities");
                if (!probs.isNull()) {
                    if (!probs.isObject() || probs.size()!=5) throw error(502,"AI_BAD_OUTPUT");
                    double sum=0; for(int i=0;i<5;i++){JsonNode p=probs.path(String.valueOf(i));optionalNumber(p,1);if(!p.isNumber())throw error(502,"AI_BAD_OUTPUT");sum+=p.asDouble();}
                    if(Math.abs(sum-1)>0.01)throw error(502,"AI_BAD_OUTPUT");
                }
                JsonNode ev = d.path("evidence"); if (!ev.isArray()) throw error(502,"AI_BAD_EVIDENCE");
                if (!ev.isEmpty()) { var src=json.createObjectNode().put("resume",resume);src.set("job",inputs.get(id)); evidence(ev,src,null); }
                else if(d.path("status").asText().equals("scored"))throw error(502,"AI_BAD_EVIDENCE");
            }
            if(names.size()!=4 && !(status.equals("failed") && names.isEmpty()))throw error(502,"AI_BAD_OUTPUT");
        }
        if(result.path("complete").asBoolean()!=complete)throw error(502,"AI_BAD_OUTPUT");
        return result;
    }
    public List<String> texts(JsonNode result, String field) {
        List<String> out=new ArrayList<>(); result.path(field).forEach(n->out.add(n.path("text").asText())); return out;
    }
    private List<String> compatible(String source, String field) {
        JsonNode r=analyze("compat",source); if(r.path("status").asText().equals("needs_review"))throw error(422,"AI_NEEDS_REVIEW"); return texts(r,field);
    }
    public List<String> extractJobRequirements(String source){return compatible(source,"requirements");}
    public List<String> extractJobBenefits(String source){return compatible(source,"benefits");}
    public List<String> coreRequirementAnalyzer(String source){return compatible(source,"coreRequirements");}
    public Integer getUserSimilarity(String source,String resume){
        var job=json.createObjectNode().put("jobId","compat");job.set("detailData",detail(source));
        JsonNode r=match(resume,List.of(job),30000);if(!r.path("complete").asBoolean())throw error(422,"AI_INSUFFICIENT_EVIDENCE");
        return r.path("items").get(0).path("score").decimalValue().setScale(0,RoundingMode.HALF_UP).intValueExact();
    }
    public static ResponseStatusException error(int status,String code){return new ResponseStatusException(HttpStatusCode.valueOf(status),code);}
}
