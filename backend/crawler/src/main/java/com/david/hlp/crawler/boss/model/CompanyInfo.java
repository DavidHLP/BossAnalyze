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
public class CompanyInfo implements Serializable {
    private static final long serialVersionUID = 1L;
        
        /**
         * 公司名称
         */
        private String companyName;
        
        /**
         * 融资阶段
         */
        private String financingStage;
        
        /**
         * 公司规模
         */
        private String companySize;
        
        /**
         * 公司所属行业
         */
        private String industry;
        
        /**
         * 公司介绍
         */
        private String companyIntro;
        
        /**
         * 法定公司名称
         */
        private String legalCompanyName;
        
        /**
         * 法定代表人
         */
        private String legalRepresentative;
        
        /**
         * 成立日期
         */
        private String establishDate;
        
        /**
         * 企业类型
         */
        private String companyType;
        
        /**
         * 经营状态
         */
        private String operationStatus;
        
        /**
         * 注册资金
         */
        private String registeredCapital;
}