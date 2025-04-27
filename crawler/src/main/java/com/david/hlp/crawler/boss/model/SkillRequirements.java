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
public class SkillRequirements implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 学历要求
     */
    private String educationLevel;
    
    /**
     * 经验年限
     */
    private Integer experienceYears;
    
    /**
     * 经验水平
     */
    private String experienceLevel;
    
    /**
     * 关键技能
     */
    private List<String> keySkills;

}