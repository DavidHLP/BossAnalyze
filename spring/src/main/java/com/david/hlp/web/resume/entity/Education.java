package com.david.hlp.web.resume.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class Education {
    private String startDate;
    private String endDate;
    private String school;
    private String major;
    private String gpa;
    private String courses;
}