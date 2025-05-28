package com.david.hlp.web.log.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 所有统计信息聚合模型类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AllStats implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private AccessLogStats summary;  // 访问日志总体统计
    private IpStats ipStats;         // IP统计信息
    private UrlStats urlStats;       // URL统计信息
    private TimeStats timeStats;     // 时间统计信息
} 