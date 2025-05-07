package com.david.hlp.crawler.ai.entity;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 职位详情精简实体类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MiniJobDetail {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 职位详情数据
     */
    private String detailData;
    
    /**
     * 员工福利
     */
    private String employeeBenefits;
    
    /**
     * 职位需求
     */
    private String jobRequirements;
}
