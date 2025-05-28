package com.david.hlp.web.log.model;

import java.io.Serializable;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * URL统计信息模型类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UrlStats implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long uniqueCount; // 唯一URL数量
    private Map<String, Integer> topUrls; // 访问量最高的URL及其访问次数
}
