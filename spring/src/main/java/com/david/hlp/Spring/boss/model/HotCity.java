package com.david.hlp.Spring.boss.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HotCity {
    private String cityName;
    private Long jobCount;
}
