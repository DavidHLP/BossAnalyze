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
public class WeekdayStatistics {
    private Map<String, DayInfo> weekdayStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayInfo {
        private Integer count;
        private Double percentage;
        private String dayName;
        private Boolean isWorkday;
        private Boolean isWeekend;
        private Double avgHourlyVisits;
    }
}
