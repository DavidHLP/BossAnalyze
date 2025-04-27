package com.david.hlp.Spring.analyzer.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.io.Serializable;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalyzerEntity implements Serializable {
    /** 职位链接 */
    private String Url;
    /** 城市 */
    private String City;
    /** 职位 */
    private String Position;
    /** 城市编码 */
    private String CityCode;
    /** 职位编码 */
    private String PositionCode;
    /** 学历 */
    private String degree;
    /** 公司地址 */
    private String BossAddress;
    /** 薪资 */
    private List<Integer> salary;
    /** 经验 */
    private List<Integer> experience;
    /** 公司福利 */
    private List<String> CompanyBenefits;
    /** 职位关键词 */
    private List<String> JobKeywords;
    /** 职位要求 */
    private String JobRequirements;
    /** 公司简介 */
    private String CompanyIntro;
    /** 公司名称 */
    private String CompanyName;
    /** 公司规模 */
    private String CompanySize;
    /** 公司类型 */
    private String CompanyType;
    /** 成立时间 */
    private String EstablishDate;
    /** 融资阶段 */
    private String FinancingStage;
    /** 运营状态 */
    private String OperationStatus;
    /** 法人公司*/
    private String LegalCompanyName;
}
