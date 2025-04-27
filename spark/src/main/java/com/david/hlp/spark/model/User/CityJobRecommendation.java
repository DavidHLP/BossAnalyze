package com.david.hlp.spark.model.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 存储职位城市推荐结果
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CityJobRecommendation {
    private String cityName;
    private Double proportion;
}
