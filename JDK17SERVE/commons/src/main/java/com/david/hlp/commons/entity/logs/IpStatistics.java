package com.david.hlp.commons.entity.logs;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpStatistics {
    private Map<String, IpInfo> ipStats;
    private List<String> localNetworks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IpInfo {
        private Integer count;
        private String countryName;
        private String city;
        private String region;
        private Double longitude;
        private Double latitude;
        private Boolean isLocal;
        private String isp;
        private String riskLevel;
    }
}
