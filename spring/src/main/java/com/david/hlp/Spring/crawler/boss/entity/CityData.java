package com.david.hlp.Spring.crawler.boss.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CityData {
    private Integer id;
    private String name;
    private Integer code;
    private String url;
}
