package com.david.hlp.Spring.crawler.boss.model;

import java.io.Serializable;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobAnalysisReport implements Serializable {
    private static final long serialVersionUID = 1L;
        
    /**
     * 薪资分析
     */
    private SalaryAnalysis salaryAnalysis;
    
    /**
     * 技能需求分析
     */
    private SkillRequirements skillRequirements;
    
    /**
     * 行业分析
     */
    private IndustryAnalysis industryAnalysis;
    
    /**
     * 福利分析
     */
    private BenefitsAnalysis benefitsAnalysis;
}