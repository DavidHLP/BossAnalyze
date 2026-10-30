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
public class HttpMethodStatistics {
    private Map<String, MethodInfo> methodStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MethodInfo {
        private Integer count;
        private Double percentage;
        private String description;
        private Boolean isSafe;
        private Boolean isIdempotent;
    }
}
