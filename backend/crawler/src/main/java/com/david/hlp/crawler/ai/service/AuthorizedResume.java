package com.david.hlp.crawler.ai.service;

import com.david.hlp.crawler.ai.entity.SimilarityRequest;
import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.*;

@Service
public class AuthorizedResume {
    private final RestTemplate http;
    private final String api;
    public AuthorizedResume(@Value("${BOSSANALYZE_API_URL:http://127.0.0.1:8080}")String api){
        this.api=api;var f=new org.springframework.http.client.SimpleClientHttpRequestFactory();f.setConnectTimeout(1000);f.setReadTimeout(2000);http=new RestTemplate(f);
    }
    public String resolve(SimilarityRequest input,HttpServletRequest request){
        if(!Boolean.TRUE.equals(request.getAttribute("boss.verified")))throw AIExtractionOfJobRequirements.error(401,"AUTH_REQUIRED");
        ResumePrivacy.requireSafeMatching();
        if(input.getUserId()!=null&&!input.getUserId().isBlank())throw AIExtractionOfJobRequirements.error(422,"USER_ID_NOT_ACCEPTED");
        String text=input.getResume();
        if(input.getResumeId()!=null&&!input.getResumeId().isBlank()){
            if(text!=null&&!text.isBlank() || !input.getResumeId().matches("[A-Za-z0-9_-]{1,100}"))throw AIExtractionOfJobRequirements.error(422,"AMBIGUOUS_RESUME");
            var headers=new HttpHeaders();headers.set("Authorization",request.getHeader("Authorization"));
            try{
                JsonNode r=http.exchange(api+"/api/resume/"+input.getResumeId(),HttpMethod.GET,new HttpEntity<>(headers),JsonNode.class).getBody();
                if(r==null || r.path("code").asInt()!=200 || !r.path("data").path("id").asText().equals(input.getResumeId()))throw AIExtractionOfJobRequirements.error(403,"RESUME_NOT_OWNED");
                text=r.path("data").path("content").textValue();
            }catch(RestClientResponseException e){
                int status=e.getStatusCode().value();
                if(status==403||status==404)throw AIExtractionOfJobRequirements.error(403,"RESUME_NOT_OWNED");
                if(status==401)throw AIExtractionOfJobRequirements.error(401,"AUTH_REQUIRED");
                throw AIExtractionOfJobRequirements.error(503,"IDENTITY_UNAVAILABLE");
            }catch(RestClientException e){throw AIExtractionOfJobRequirements.error(503,"IDENTITY_UNAVAILABLE");}
        }
        if(text==null||text.isBlank()||text.length()>32000)throw AIExtractionOfJobRequirements.error(422,"INVALID_RESUME");
        // shortcut: plain-text resumes use labeled-line redaction; upgrade with a structured resume schema.
        return text.replaceAll("(?im)^\\s*(?:姓名|电话|手机|邮箱|微信|性别|民族|年龄|出生日期|住址|name|phone|email|gender|age)\\s*[:：].*(?:\\R|$)","")
                .replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}","[redacted]")
                .replaceAll("(?<![0-9])1[3-9][0-9]{9}(?![0-9])","[redacted]");
    }
}
