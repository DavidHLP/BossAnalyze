package com.david.hlp.crawler.ai.entity;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserSimilarity {
    private Long id;
    private Integer similarity;
    private MiniJobDetail miniJobDetail;
    private JobAnalysisData jobAnalysisData;
}
