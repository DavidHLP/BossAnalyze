package com.david.hlp.web.boss.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalaryJob implements Serializable {
    private String positionName;
    private Long jobCount;
    private Long minSalary;
    private Long maxSalary;
    private Double avgSalary;
    private String recommendedCity;
    private Double recommendedCitySalary;
    private Long recommendedCityJobCount;
}