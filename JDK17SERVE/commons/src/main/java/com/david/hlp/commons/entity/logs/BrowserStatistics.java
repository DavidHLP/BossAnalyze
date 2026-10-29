package com.david.hlp.commons.entity.logs;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowserStatistics {
    private Map<String, BrowserInfo> browserStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrowserInfo {
        private Integer count;
        private Double percentage;
        private String version;
        private Map<String, Integer> osDistribution;
        private Map<String, Integer> deviceTypeDistribution;
        private Integer marketRank;
        private Boolean isModern;
    }
}
