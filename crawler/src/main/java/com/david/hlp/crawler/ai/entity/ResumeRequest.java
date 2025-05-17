package com.david.hlp.crawler.ai.entity;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResumeRequest {
    private String resume;
    private String city;
    private String position;
}
