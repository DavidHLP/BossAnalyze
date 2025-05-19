package com.david.hlp.web.resume.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "resumes")
@CompoundIndexes({
    @CompoundIndex(name = "userId_createdAt", def = "{'userId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "userId_updatedAt", def = "{'userId': 1, 'updatedAt': -1}"),
    @CompoundIndex(name = "userName_createdAt", def = "{'userName': 1, 'createdAt': -1}")
})
public class Resume {
    @Id
    private String id;

    private String userId;
    private String userName;

    private String name;
    private String age;
    private String gender;
    private String location;
    private String experience;
    private String phone;
    private String email;
    private String avatar;
    private String jobTarget;
    private String expectedSalary;
    private String targetCity;
    private String availableTime;

    private List<Education> education;
    private List<WorkExperience> workExperience;
    private List<String> interestTags;
    private String selfEvaluation;
    private List<CustomSkill> customSkills;
    private List<Certificate> certificates;
    private List<String> sectionOrder;
    @CreatedDate
    private Date createdAt;
    @LastModifiedDate
    private Date updatedAt;
}
