package com.david.hlp.spark.model.Log;

import java.io.Serializable;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 时间统计信息模型类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimeStats implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Integer peakHour;                        // 访问高峰时间（小时）
    private Map<String, Object> hourlyDistribution;  // 每小时访问量分布
}
