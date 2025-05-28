package com.david.hlp.crawler.ai.entity;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SimilarityRequest {
    private String resumeId;
    private String resume;
    private List<String> city;
    private String position;
    private String userId;
}
