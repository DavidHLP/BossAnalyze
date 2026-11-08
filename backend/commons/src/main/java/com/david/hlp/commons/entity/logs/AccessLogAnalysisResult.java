package com.david.hlp.commons.entity.logs;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessLogAnalysisResult {
    private LocalDateTime analysisTime;
    private LocalDateTime dataStartTime;
    private LocalDateTime dataEndTime;
    private long totalVisits;
    private long uniqueVisitors;
    private HeatMapData heatMapData;
    private IpStatistics ipStatistics;
    private HttpMethodStatistics httpMethodStatistics;
    private WeekdayStatistics weekdayStatistics;
    private BrowserStatistics browserStatistics;
    private DataQualityInfo dataQuality;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataQualityInfo {
        private long totalLogLines;
        private long parsedLines;
        private long failedLines;
        private Double parseSuccessRate;
        private Integer qualityScore;
    }
}
