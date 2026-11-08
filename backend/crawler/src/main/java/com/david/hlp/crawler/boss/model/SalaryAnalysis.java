package com.david.hlp.crawler.boss.model;

import java.io.Serializable;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalaryAnalysis implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 最低薪资
     */
    private Double minSalary;
    
    /**
     * 最高薪资
     */
    private Double maxSalary;
    
    /**
     * 平均薪资
     */
    private Double avgSalary;
    
    /**
     * 估算年薪
     */
    private Double estimatedAnnualSalary;
    
    /**
     * 薪资水平
     */
    private String salaryLevel;
}