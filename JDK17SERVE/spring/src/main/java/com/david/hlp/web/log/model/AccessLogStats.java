package com.david.hlp.web.log.model;

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
    private Long uniquePaths; // 唯一路径数
    private Map<String, Integer> apiCategoryStats;
    private Map<String, Integer> popularPaths;
}
