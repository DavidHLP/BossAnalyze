package com.david.hlp.kafka.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Degree {
    private Integer id;
    private String uniqueId;
    private String city;
    private String degree;
    private String salary;
    private String experience;
    private String updateTime;
}
