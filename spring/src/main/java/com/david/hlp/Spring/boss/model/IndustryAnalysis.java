package com.david.hlp.Spring.boss.model;

import java.io.Serializable;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IndustryAnalysis implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 所属行业
     */
    private String industry;
    
    /**
     * 行业前景
     */
    private String outlook;
    
    /**
     * 增长评估
     */
    private String growthRate;
}