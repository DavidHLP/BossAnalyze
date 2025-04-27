package com.david.hlp.spark.model.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CityHotJob {
    private String cityName;
    private String positionName;
    private Long jobCount;
}
