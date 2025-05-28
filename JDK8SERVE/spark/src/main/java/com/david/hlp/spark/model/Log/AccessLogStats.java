package com.david.hlp.spark.model.Log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.Map;

/**
 * 访问日志总体统计信息模型类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccessLogStats implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long totalRequests; // 总请求数
    private Long uniqueIPs; // 唯一IP数
    private Map<String, Object> topUrls; // 热门URL列表
    private Integer peakHour; // 访问高峰时间（小时）
}
