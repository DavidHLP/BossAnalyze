package com.david.hlp.Spring.crawler.boss.model;

import java.util.List;
import java.io.Serializable;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
/**
 * 职位详情数据模型
 * 用于存储解析后的职位信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobDetailData implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 页面更新时间
     */
    private String updateTime;
    /**
     * 基本职位信息
     */
    private BasicJobInfo basicInfo;
    /**
     * 职位描述信息
     */
    private JobDescriptionInfo jobDescription;
    /**
     * 公司信息
     */
    private CompanyInfo companyInfo;
    /**
     * 职位福利标签
     */
    private List<String> jobTags;
    /**
     * 职业分析报告
     */
    private JobAnalysisReport analysisReport;
}