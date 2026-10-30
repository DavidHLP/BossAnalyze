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
public class HeatMapData {
    private Map<String, TimePointInfo> minuteHeatmap;
    private Map<String, TimePointInfo> hourHeatmap;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimePointInfo {
        private Integer count;
        private String intensity;
    }
}
