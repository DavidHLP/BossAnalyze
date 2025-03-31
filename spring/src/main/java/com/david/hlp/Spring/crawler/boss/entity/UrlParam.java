package com.david.hlp.Spring.crawler.boss.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlParam {
    private String baseCity;
    private String basePosition;
    private String baseCityCode;
    private String basePositionCode;
    private String baseType;
}
