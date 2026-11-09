package com.david.hlp.crawler.ai.service;

import java.util.*;
import java.math.RoundingMode;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.david.hlp.crawler.ai.entity.*;
import com.david.hlp.crawler.ai.mapper.TJobDetailMapper;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RequirementAnalyzer {
    private final TJobDetailMapper jobs;
    private final AIExtractionOfJobRequirements ai;
    private final FormatDataService format;
    private final ObjectMapper json;
    public ObjectNode matches(String resume,List<String> cities,String position) {
        if(resume==null || resume.isBlank() || resume.length()>32000 || cities==null || cities.isEmpty() || cities.size()>10
                || cities.stream().anyMatch(c->c==null||c.isBlank()||c.length()>100) || position==null || position.isBlank() || position.length()>200) throw AIExtractionOfJobRequirements.error(422,"AI_INVALID_INPUT");
        // Fetch one extra row to report range incompleteness without an unbounded scan.
        Map<Long,MiniJobDetail> unique=new TreeMap<>();
        for(String city:new LinkedHashSet<>(cities)) for(var job:jobs.selectMiniJobDetail(city,position,0L,101)) unique.put(job.getId(),job);
        boolean bounded=unique.size()<=100;
        List<MiniJobDetail> selected=unique.values().stream().limit(100).toList();
        List<JsonNode> scored=new ArrayList<>(), unrankable=new ArrayList<>();
        long deadline=System.nanoTime()+30_000_000_000L;
        for(int i=0;i<selected.size();i+=10){
            List<ObjectNode> batch=new ArrayList<>();
            for(var job:selected.subList(i,Math.min(i+10,selected.size()))){var n=json.createObjectNode().put("jobId",job.getId().toString());n.set("detailData",ai.detail(job.getDetailData()));batch.add(n);}
            JsonNode response=ai.match(resume,batch,(int)Math.max(0,(deadline-System.nanoTime())/1_000_000));
            for(JsonNode item:response.path("items")) {
                if(item.path("status").asText().equals("scored"))scored.add(item);else unrankable.add(item);
            }
        }
        scored.sort((a,b)->{int c=b.path("score").decimalValue().compareTo(a.path("score").decimalValue());return c!=0?c:Long.compare(Long.parseLong(a.path("jobId").asText()),Long.parseLong(b.path("jobId").asText()));});
        var out=json.createObjectNode();var items=out.putArray("items");
        for(JsonNode score:scored.stream().limit(10).toList()){
            MiniJobDetail job=unique.get(Long.valueOf(score.path("jobId").asText()));
            var n=(ObjectNode)score.deepCopy();var data=format.convertToJobAnalysisData(job);data.setJobUrl(job.getUrl());n.set("jobAnalysisData",json.valueToTree(data));items.add(n);
        }
        out.set("unrankable",json.valueToTree(unrankable));out.put("consideredCount",selected.size());out.put("complete",bounded&&unrankable.isEmpty());
        out.put("model",ai.model());out.put("policyVersion","jev-policy-1");
        var scope=out.putObject("scope");scope.set("cities",json.valueToTree(cities));scope.put("position",position);scope.put("limit",100);scope.put("truncated",!bounded);
        return out;
    }
    public List<UserSimilarity> getUserSimilarity(String resume,List<String> cities,String position) {
        JsonNode out=matches(resume,cities,position);
        for(JsonNode n:out.path("unrankable"))if(n.path("status").asText().equals("failed"))throw AIExtractionOfJobRequirements.error(503,"AI_UNAVAILABLE");
        if(!out.path("complete").asBoolean())throw AIExtractionOfJobRequirements.error(422,"AI_INSUFFICIENT_EVIDENCE_OR_RANGE");
        List<UserSimilarity> result=new ArrayList<>();
        for(JsonNode n:out.path("items")) result.add(UserSimilarity.builder().id(Long.valueOf(n.path("jobId").asText()))
                .similarity(n.path("score").decimalValue().setScale(0,RoundingMode.HALF_UP).intValueExact())
                .jobAnalysisData(json.convertValue(n.path("jobAnalysisData"),JobAnalysisData.class)).build());
        return result;
    }
}
