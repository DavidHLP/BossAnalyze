package com.david.hlp.Spring.analyzer.service;

import com.google.gson.Gson;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import com.david.hlp.Spring.analyzer.entity.AnalyzerEntity;

public class SalaryAnalyzer {

    public AnalyzerEntity analyzeSalary(String Url , String Json , String City , String Position , String CityCode , String PositionCode) {
        Gson gson = new Gson();
        @SuppressWarnings("unchecked")
        Map<String, Object> jobData = gson.fromJson(Json, Map.class);
        AnalyzerEntity analyzerEntity = extractJobInfo(jobData);
        analyzerEntity.setUrl(Url);
        analyzerEntity.setCity(City);
        analyzerEntity.setPosition(Position);
        analyzerEntity.setCityCode(CityCode);
        analyzerEntity.setPositionCode(PositionCode);
        return analyzerEntity;
    }

    private AnalyzerEntity extractJobInfo(Map<String, Object> jobData) {
        AnalyzerEntity entity = new AnalyzerEntity();
        // 处理基本信息
        @SuppressWarnings("unchecked")
        Map<String, Object> basicInfo = (Map<String, Object>) jobData.get("basicInfo");
        if (basicInfo != null) {
            entity.setDegree((String) basicInfo.get("degree"));
            entity.setBossAddress((String) basicInfo.get("address"));
            // 处理薪资范围，格式如"6-11K"
            String salaryStr = (String) basicInfo.get("salary");
            if (salaryStr != null && !salaryStr.isEmpty()
                    && (salaryStr.contains("K") || salaryStr.contains("千") || salaryStr.contains("k"))) {
                List<Integer> salaryRange = parseSalaryRange(salaryStr);
                entity.setSalary(salaryRange);
            }
            // 处理经验要求，格式如"1-3年"
            String experienceStr = (String) basicInfo.get("experience");
            if (experienceStr != null && !experienceStr.isEmpty()) {
                List<Integer> experienceRange = parseExperienceRange(experienceStr);
                entity.setExperience(experienceRange);
            }
        }
        // 提取公司福利
        @SuppressWarnings("unchecked")
        List<String> jobTags = (List<String>) jobData.get("jobTags");
        entity.setCompanyBenefits(jobTags);
        // 提取职位关键词
        @SuppressWarnings("unchecked")
        Map<String, Object> jobDescription = (Map<String, Object>) jobData.get("jobDescription");
        if (jobDescription != null) {
            @SuppressWarnings("unchecked")
            List<String> keywords = (List<String>) jobDescription.get("keywords");
            entity.setJobKeywords(keywords);
            // 提取职位要求
            String responsibilities = (String) jobDescription.get("responsibilities");
            entity.setJobRequirements(responsibilities);
        }
        // 提取公司信息
        @SuppressWarnings("unchecked")
        Map<String, Object> companyInfo = (Map<String, Object>) jobData.get("companyInfo");
        if (companyInfo != null) {
            entity.setCompanyIntro((String) companyInfo.get("companyIntro"));
            entity.setCompanyName((String) companyInfo.get("companyName"));
            entity.setCompanySize((String) companyInfo.get("companySize"));
            entity.setCompanyType((String) companyInfo.get("companyType"));
            entity.setEstablishDate((String) companyInfo.get("establishDate"));
            entity.setFinancingStage((String) companyInfo.get("financingStage"));
            entity.setOperationStatus((String) companyInfo.get("operationStatus"));
            entity.setLegalCompanyName((String) companyInfo.get("legalCompanyName"));
        }
        return entity;
    }

    private List<Integer> parseSalaryRange(String salaryStr) {
        List<Integer> result = new ArrayList<>();
        // 移除"K"后缀并按"-"分割
        String processedStr = salaryStr.replace("K", "").replace("千", "").replace("k", "").trim();
        String[] parts = processedStr.split("-");

        try {
            if (parts.length >= 2) {
                int start = Integer.parseInt(parts[0]);
                int end = Integer.parseInt(parts[1]);
                // 记录范围内所有薪资值
                for (int i = start; i <= end; i++) {
                    result.add(i);
                }
            } else if (parts.length == 1) {
                result.add(Integer.parseInt(parts[0]));
            }
        } catch (NumberFormatException e) {
            // 解析失败时返回空列表
            return new ArrayList<>();
        }
        return result;
    }

    private List<Integer> parseExperienceRange(String experienceStr) {
        List<Integer> result = new ArrayList<>();
        // 移除"年"后缀并按"-"分割
        String processedStr = experienceStr.replace("年", "").trim();
        String[] parts = processedStr.split("-");
        try {
            if (parts.length >= 2) {
                int start = Integer.parseInt(parts[0]);
                int end = Integer.parseInt(parts[1]);
                // 记录范围内所有经验年限值
                for (int i = start; i <= end; i++) {
                    result.add(i);
                }
            } else if (parts.length == 1) {
                result.add(Integer.parseInt(parts[0]));
            }
        } catch (NumberFormatException e) {
            // 解析失败时返回空列表
            return new ArrayList<>();
        }
        return result;
    }
}
