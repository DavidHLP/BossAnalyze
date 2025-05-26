package com.david.hlp.crawler.boss.model;

import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BenefitsAnalysis implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 福利数量
     */
    private Integer benefitsCount;
    
    /**
     * 所有福利
     */
    private List<String> allBenefits;
    
    /**
     * 保险相关福利
     */
    private List<String> insuranceBenefits;
    
    /**
     * 奖金相关福利
     */
    private List<String> bonusBenefits;
    
    /**
     * 工作生活平衡福利
     */
    private List<String> workLifeBalanceBenefits;
    
    /**
     * 发展相关福利
     */
    private List<String> developmentBenefits;
    
    /**
     * 设施相关福利
     */
    private List<String> facilitiesBenefits;
    
    /**
     * 福利评级
     */
    private String benefitsRating;
}
