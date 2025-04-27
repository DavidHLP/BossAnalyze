package com.david.hlp.spark.model.User;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 存储职位需求分析结果
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobRequirement {
    private String positionName;
    private List<Map<String, Long>> keywordCounts;
}
