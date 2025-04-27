package com.david.hlp.spark.model.User;

import java.io.Serializable;
import java.util.Map;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;


/**
 * 职位关键词分析结果
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobKeywordResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 职位名称
     */
    private String positionName;
    
    /**
     * 关键词统计结果，key为关键词，value为出现次数
     */
    private Map<String, Integer> keywordCount;
} 