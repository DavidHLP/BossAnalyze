package com.david.hlp.web.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobAnalysisData {
    private String positionId;
    private String positionName;
    private String cityName;
    private String salary;
    private Integer salaryValue;
    private String degree;
    private Integer degreeValue;
    private String experience;
    private Integer experienceValue;
    private String companyName;
    private String companySize;
    private String financingStage;
    private String companyUrl;
    private String JobUrl;
    private String address;
    private List<String> employeeBenefits;
    private List<String> jobRequirements;
}
