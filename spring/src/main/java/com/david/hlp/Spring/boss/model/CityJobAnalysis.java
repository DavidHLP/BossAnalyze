package com.david.hlp.Spring.boss.model;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CityJobAnalysis {
    private String cityName;
    private String positionName;
    private List<Map<String, Long>> keywordCounts;
}

