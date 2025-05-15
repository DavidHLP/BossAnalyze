package com.david.hlp.web.resume.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class WorkExperience {
    private String startDate;
    private String endDate;
    private String company;
    private String position;
    private List<String> duties;
}