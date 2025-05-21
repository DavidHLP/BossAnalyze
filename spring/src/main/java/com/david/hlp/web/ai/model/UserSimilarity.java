package com.david.hlp.web.ai.model;

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
    private String cityName;
    private String positionName;
    private Integer similarity;
    private MiniJobDetail miniJobDetail;
    private JobAnalysisData jobAnalysisData;
}
