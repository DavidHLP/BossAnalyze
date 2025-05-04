package com.david.hlp.Spring.boss.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalaryJob {
    private String positionName;
    private Long jobCount;
    private Long minSalary;
    private Long maxSalary;
    private Double avgSalary;
    private String recommendedCity;
    private Double recommendedCitySalary;
    private Long recommendedCityJobCount;
}