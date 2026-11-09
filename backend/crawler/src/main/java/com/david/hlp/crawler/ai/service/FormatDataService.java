package com.david.hlp.crawler.ai.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.david.hlp.crawler.ai.entity.JobAnalysisData;
import com.david.hlp.crawler.ai.entity.MiniJobDetail;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.david.hlp.commons.recruitment.JobArrays;

@Service
@RequiredArgsConstructor
public class FormatDataService {

    private final ObjectMapper objectMapper;

    public JobAnalysisData convertToJobAnalysisData(MiniJobDetail miniJobDetail) {
        if (miniJobDetail == null) {
            return null;
        }

        try {
            // 解析detailData中的JSON数据
            JsonNode rootNode = objectMapper.readTree(miniJobDetail.getDetailData());

            // 构建JobAnalysisData对象
            JobAnalysisData jobAnalysisData = new JobAnalysisData();

            // 设置职位ID
            jobAnalysisData.setPositionId(String.valueOf(miniJobDetail.getId()));

            // 解析基本信息
            if (rootNode.has("basicInfo")) {
                JsonNode basicInfo = rootNode.get("basicInfo");
                if (basicInfo.has("positionName")) {
                    jobAnalysisData.setPositionName(basicInfo.get("positionName").asText());
                }
                if (basicInfo.has("city")) {
                    jobAnalysisData.setCityName(basicInfo.get("city").asText());
                }
                if (basicInfo.has("salary")) {
                    String salary = basicInfo.get("salary").asText();
                    jobAnalysisData.setSalary(salary);
                    // 提取薪资值，例如从"10-15K·13薪"提取整数值
                    jobAnalysisData.setSalaryValue(extractSalaryValue(salary));
                }
                if (basicInfo.has("degree")) {
                    String degree = basicInfo.get("degree").asText();
                    jobAnalysisData.setDegree(degree);
                    jobAnalysisData.setDegreeValue(convertDegreeToValue(degree));
                }
                if (basicInfo.has("experience")) {
                    String experience = basicInfo.get("experience").asText();
                    jobAnalysisData.setExperience(experience);
                    jobAnalysisData.setExperienceValue(extractExperienceValue(experience));
                }
                if (basicInfo.has("address")) {
                    jobAnalysisData.setAddress(basicInfo.get("address").asText());
                }
            }

            // 解析公司信息
            if (rootNode.has("companyInfo")) {
                JsonNode companyInfo = rootNode.get("companyInfo");
                if (companyInfo.has("companyName")) {
                    jobAnalysisData.setCompanyName(companyInfo.get("companyName").asText());
                }
                if (companyInfo.has("companySize")) {
                    jobAnalysisData.setCompanySize(companyInfo.get("companySize").asText());
                }
                if (companyInfo.has("financingStage")) {
                    jobAnalysisData.setFinancingStage(companyInfo.get("financingStage").asText());
                }
            }

            // 解析URL信息
            if (rootNode.has("companyUrl")) {
                jobAnalysisData.setCompanyUrl(rootNode.get("companyUrl").asText());
            }

            jobAnalysisData.setEmployeeBenefits(JobArrays.read(miniJobDetail.getEmployeeBenefits()));
            jobAnalysisData.setJobRequirements(JobArrays.read(miniJobDetail.getJobRequirements()));

            jobAnalysisData.setPositionName(miniJobDetail.getPositionName());
            jobAnalysisData.setCityName(miniJobDetail.getCityName());

            return jobAnalysisData;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 提取薪资范围的平均值
    private Integer extractSalaryValue(String salary) {
        if (salary == null)
            return 0;

        try {
            // 匹配如"10-15K·13薪"中的数字
            Pattern pattern = Pattern.compile("(\\d+)-(\\d+)");
            Matcher matcher = pattern.matcher(salary);

            if (matcher.find()) {
                int min = Integer.parseInt(matcher.group(1));
                int max = Integer.parseInt(matcher.group(2));
                return (min + max) / 2;
            }
        } catch (Exception e) {
            // 忽略异常，返回默认值
        }

        return 0;
    }

    // 将学历转换为数值
    private Integer convertDegreeToValue(String degree) {
        if (degree == null)
            return 0;

        switch (degree) {
            case "初中及以下":
                return 1;
            case "高中":
                return 2;
            case "中专/技校":
                return 3;
            case "大专":
                return 4;
            case "本科":
                return 5;
            case "硕士":
                return 6;
            case "博士":
                return 7;
            default:
                return 0;
        }
    }

    // 提取工作经验值
    private Integer extractExperienceValue(String experience) {
        if (experience == null)
            return 0;

        try {
            // 匹配如"3-5年"中的数字
            Pattern pattern = Pattern.compile("(\\d+)-(\\d+)");
            Matcher matcher = pattern.matcher(experience);

            if (matcher.find()) {
                int min = Integer.parseInt(matcher.group(1));
                int max = Integer.parseInt(matcher.group(2));
                return (min + max) / 2;
            } else if (experience.contains("应届")) {
                return 0;
            } else if (experience.contains("无需经验")) {
                return 0;
            } else if (experience.contains("10年以上")) {
                return 10;
            }

            // 匹配如"5年以上"的模式
            pattern = Pattern.compile("(\\d+)年以上");
            matcher = pattern.matcher(experience);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            // 忽略异常，返回默认值
        }

        return 0;
    }
}
