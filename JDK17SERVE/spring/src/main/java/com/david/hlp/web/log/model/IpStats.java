package com.david.hlp.web.log.model;

import java.io.Serializable;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * IP统计信息模型类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IpStats implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long uniqueCount; // 唯一IP数量
    private Map<String, Integer> topIPs; // 访问量最高的IP及其访问次数
}
