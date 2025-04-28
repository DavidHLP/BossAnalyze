package com.david.hlp.Spring.boss.model;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobRequirement {
    private String positionName;
    private List<Map<String, Long>> keywordCounts;
}

