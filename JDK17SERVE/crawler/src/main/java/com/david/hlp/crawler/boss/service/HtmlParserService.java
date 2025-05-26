package com.david.hlp.crawler.boss.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import com.david.hlp.crawler.boss.model.BasicJobInfo;
import com.david.hlp.crawler.boss.model.BenefitsAnalysis;
import com.david.hlp.crawler.boss.model.CompanyInfo;
import com.david.hlp.crawler.boss.model.IndustryAnalysis;
import com.david.hlp.crawler.boss.model.JobAnalysisReport;
import com.david.hlp.crawler.boss.model.JobDescriptionInfo;
import com.david.hlp.crawler.boss.model.JobDetailData;
import com.david.hlp.crawler.boss.model.SalaryAnalysis;
import com.david.hlp.crawler.boss.model.SkillRequirements;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;

/**
 * IP访问异常异常
 */
class IpBlockedException extends IOException {
    public IpBlockedException(String message) {
        super(message);
    }
}

/**
 * 职位详情页HTML解析服务
 */
@Service
@Slf4j
public class HtmlParserService {
    private static final String IP_BLOCK_MESSAGE = "当前IP地址可能存在异常访问行为，完成验证后即可正常使用.";

    /**
     * 解析职位详情HTML字符串
     */
    public JobDetailData parseJobDetail(String htmlContent) throws IOException {
        if (htmlContent == null || htmlContent.isEmpty()) {
            throw new IllegalArgumentException("HTML内容不能为空");
        }

        try {
            // 检查HTML内容是否包含IP限制信息
            if (htmlContent.contains(IP_BLOCK_MESSAGE)) {
                log.warn("检测到IP被限制访问：{}", IP_BLOCK_MESSAGE);
                throw new IpBlockedException("IP访问受限，需要完成验证：" + IP_BLOCK_MESSAGE);
            }

            Document doc = Jsoup.parse(htmlContent);
            Map<String, Object> result = new HashMap<>();

            // 提取更新时间
            extractElementText(doc, ".gray", "updateTime", result, text -> text.replace("页面更新时间：", "").trim());

            // 提取基本职位信息
            Map<String, String> basicInfo = new HashMap<>();
            extractElementText(doc, ".job-primary .name h1", "positionName", basicInfo);
            extractElementText(doc, ".job-primary .name .salary", "salary", basicInfo);
            extractElementText(doc, ".text-city", "city", basicInfo);
            extractElementText(doc, ".text-experiece", "experience", basicInfo);
            extractElementText(doc, ".text-degree", "degree", basicInfo);
            extractElementText(doc, ".location-address", "address", basicInfo);
            result.put("basicInfo", basicInfo);

            // 提取职位描述和要求
            extractJobDescription(doc, result);

            // 提取公司信息
            Map<String, String> companyInfo = new HashMap<>();
            extractCompanyNameAndUrl(doc, companyInfo);
            extractElementWithIcon(doc, ".sider-company p:has(.icon-stage)", "financingStage", companyInfo);
            extractElementWithIcon(doc, ".sider-company p:has(.icon-scale)", "companySize", companyInfo);
            extractElementText(doc, ".sider-company p:has(.icon-industry) a", "industry", companyInfo);
            extractElementText(doc, ".job-detail-company .fold-text", "companyIntro", companyInfo);
            extractBusinessInfo(doc, companyInfo);
            result.put("companyInfo", companyInfo);

            // 提取职位福利标签
            List<String> tags = new ArrayList<>();
            Elements tagElements = doc.select(".job-tags");
            if (!tagElements.isEmpty()) {
                Element firstTag = tagElements.get(0);
                if (firstTag != null) {
                    for (Element element : firstTag.select("span")) {
                        tags.add(element.text());
                    }
                }
            }
            result.put("jobTags", tags);

            // 清洗数据
            cleanJobDescriptionData(result);
            removeDuplicateTags(result);
            ensureCompanyName(result);

            // 转换为JobDetailData对象
            return convertToJobDetailData(result);
        } catch (Exception e) {
            log.error("解析职位详情HTML时发生错误: {}", e.getMessage(), e);
            throw new IOException("解析职位详情失败", e);
        }
    }

    /**
     * 提取职位描述和要求
     */
    private void extractJobDescription(Document doc, Map<String, Object> result) {
        Map<String, Object> jobDescription = new HashMap<>();
        Element descElement = doc.selectFirst(".job-sec-text");

        if (descElement != null) {
            String fullText = descElement.html();
            if (fullText.contains("任职要求")) {
                String[] parts = fullText.split("岗位职责");
                if (parts.length > 0) {
                    jobDescription.put("requirements", parts[0].replace("任职要求：", "").trim());
                }
                if (parts.length > 1) {
                    jobDescription.put("responsibilities", parts[1].trim());
                }
            } else {
                jobDescription.put("fullDescription", fullText);
            }
        }

        // 关键词标签
        List<String> keywords = new ArrayList<>();
        for (Element element : doc.select(".job-keyword-list li")) {
            keywords.add(element.text());
        }
        jobDescription.put("keywords", keywords);
        result.put("jobDescription", jobDescription);
    }

    /**
     * 清洗职位描述数据
     */
    private void cleanJobDescriptionData(Map<String, Object> result) {
        if (result.get("jobDescription") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> jobDescriptionMap = (Map<String, Object>) result.get("jobDescription");
            cleanMapStringValue(jobDescriptionMap, "requirements");
            cleanMapStringValue(jobDescriptionMap, "responsibilities");
            cleanMapStringValue(jobDescriptionMap, "fullDescription");
        }
    }

    /**
     * 清洗Map中的字符串值
     */
    private void cleanMapStringValue(Map<String, Object> map, String key) {
        if (map.containsKey(key)) {
            map.put(key, cleanHtmlTags((String) map.get(key)));
        }
    }

    /**
     * 去除职位福利标签的重复
     */
    private void removeDuplicateTags(Map<String, Object> result) {
        if (result.get("jobTags") instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> jobTags = (List<String>) result.get("jobTags");
            if (!jobTags.isEmpty()) {
                result.put("jobTags", new ArrayList<>(new LinkedHashSet<>(jobTags)));
            }
        }
    }

    /**
     * 确保公司名称不为空
     */
    private void ensureCompanyName(Map<String, Object> result) {
        if (result.get("companyInfo") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> companyInfoMap = (Map<String, String>) result.get("companyInfo");
            if ((companyInfoMap.get("companyName") == null || companyInfoMap.get("companyName").isEmpty())
                    && companyInfoMap.containsKey("legalCompanyName")
                    && companyInfoMap.get("legalCompanyName") != null) {
                companyInfoMap.put("companyName", companyInfoMap.get("legalCompanyName"));
            }
        }
    }

    /**
     * 将Map数据转换为JobDetailData对象
     */
    private JobDetailData convertToJobDetailData(Map<String, Object> result) {
        JobDetailData jobDetailData = new JobDetailData();
        jobDetailData.setUpdateTime(convertEmptyToNull((String) result.get("updateTime")));

        // 转换基本信息
        convertBasicInfo(result, jobDetailData);

        // 转换职位描述
        convertJobDescription(result, jobDetailData);

        // 转换公司信息
        convertCompanyInfo(result, jobDetailData);

        // 设置职位标签
        if (result.get("jobTags") instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> jobTags = (List<String>) result.get("jobTags");
            jobDetailData.setJobTags(jobTags.isEmpty() ? null : jobTags);
        }

        return jobDetailData;
    }

    /**
     * 转换基本信息
     */
    private void convertBasicInfo(Map<String, Object> result, JobDetailData jobDetailData) {
        if (result.get("basicInfo") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> basicInfoMap = (Map<String, String>) result.get("basicInfo");
            BasicJobInfo basicJobInfo = new BasicJobInfo();
            basicJobInfo.setPositionName(convertEmptyToNull(basicInfoMap.get("positionName")));
            basicJobInfo.setSalary(convertEmptyToNull(basicInfoMap.get("salary")));
            basicJobInfo.setCity(convertEmptyToNull(basicInfoMap.get("city")));
            basicJobInfo.setExperience(convertEmptyToNull(basicInfoMap.get("experience")));
            basicJobInfo.setDegree(convertEmptyToNull(basicInfoMap.get("degree")));
            basicJobInfo.setAddress(convertEmptyToNull(basicInfoMap.get("address")));
            jobDetailData.setBasicInfo(basicJobInfo);
        }
    }

    /**
     * 转换职位描述
     */
    private void convertJobDescription(Map<String, Object> result, JobDetailData jobDetailData) {
        if (result.get("jobDescription") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> jobDescMap = (Map<String, Object>) result.get("jobDescription");
            JobDescriptionInfo jobDesc = new JobDescriptionInfo();
            jobDesc.setRequirements(convertEmptyToNull((String) jobDescMap.get("requirements")));
            jobDesc.setResponsibilities(convertEmptyToNull((String) jobDescMap.get("responsibilities")));
            jobDesc.setFullDescription(convertEmptyToNull((String) jobDescMap.get("fullDescription")));

            if (jobDescMap.get("keywords") instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> keywordsList = (List<String>) jobDescMap.get("keywords");
                jobDesc.setKeywords(keywordsList.isEmpty() ? null : keywordsList);
            }
            jobDetailData.setJobDescription(jobDesc);
        }
    }

    /**
     * 转换公司信息
     */
    private void convertCompanyInfo(Map<String, Object> result, JobDetailData jobDetailData) {
        if (result.get("companyInfo") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> companyInfoMap = (Map<String, String>) result.get("companyInfo");
            CompanyInfo companyInfoData = new CompanyInfo();
            companyInfoData.setCompanyName(convertEmptyToNull(companyInfoMap.get("companyName")));
            companyInfoData.setFinancingStage(convertEmptyToNull(companyInfoMap.get("financingStage")));
            companyInfoData.setCompanySize(convertEmptyToNull(companyInfoMap.get("companySize")));
            companyInfoData.setIndustry(convertEmptyToNull(companyInfoMap.get("industry")));
            companyInfoData.setCompanyIntro(convertEmptyToNull(companyInfoMap.get("companyIntro")));
            companyInfoData.setLegalCompanyName(convertEmptyToNull(companyInfoMap.get("legalCompanyName")));
            companyInfoData.setLegalRepresentative(convertEmptyToNull(companyInfoMap.get("legalRepresentative")));
            companyInfoData.setEstablishDate(convertEmptyToNull(companyInfoMap.get("establishDate")));
            companyInfoData.setCompanyType(convertEmptyToNull(companyInfoMap.get("companyType")));
            companyInfoData.setOperationStatus(convertEmptyToNull(companyInfoMap.get("operationStatus")));
            companyInfoData.setRegisteredCapital(convertEmptyToNull(companyInfoMap.get("registeredCapital")));
            jobDetailData.setCompanyInfo(companyInfoData);

            String companyUrl = companyInfoMap.get("companyUrl");
            if (companyUrl != null && !companyUrl.isEmpty()) {
                jobDetailData.setCompanyUrl(convertEmptyToNull("https://www.zhipin.com" + companyUrl));
            }
        }
    }

    /**
     * 提取元素文本并添加到Map
     */
    private void extractElementText(Document doc, String selector, String key, Map<String, String> map) {
        Element element = doc.selectFirst(selector);
        if (element != null) {
            map.put(key, element.text());
        }
    }

    /**
     * 提取元素文本并添加到Map（带文本处理）
     */
    private <T> void extractElementText(Document doc, String selector, String key, Map<String, T> map,
            java.util.function.Function<String, T> processor) {
        Element element = doc.selectFirst(selector);
        if (element != null) {
            map.put(key, processor.apply(element.text()));
        }
    }

    /**
     * 提取公司名称和URL
     */
    private void extractCompanyNameAndUrl(Document doc, Map<String, String> companyInfo) {
        // 优先从标准位置获取
        Element companyNameElement = doc.selectFirst(".sider-company .company-info a");
        if (companyNameElement != null) {
            companyInfo.put("companyName", companyNameElement.text());
            String href = companyNameElement.attr("href");
            if (!href.isEmpty()) {
                companyInfo.put("companyUrl", href);
            }
            return;
        }

        // 尝试从公司详情部分获取
        Element altCompanyNameElement = doc.selectFirst(".job-detail-company .level-list .company-name");
        if (altCompanyNameElement != null) {
            companyInfo.put("companyName", altCompanyNameElement.text().replace("公司名称", "").trim());
            return;
        }

        // 尝试从职位标题区域获取
        Element titleCompanyElement = doc.selectFirst(".company-info");
        if (titleCompanyElement != null) {
            companyInfo.put("companyName", titleCompanyElement.text().trim());
            Element companyLinkElement = titleCompanyElement.selectFirst("a[ka=job-detail-company_custompage]");
            if (companyLinkElement != null) {
                String href = companyLinkElement.attr("href");
                if (!href.isEmpty()) {
                    companyInfo.put("companyUrl", href);
                }
            }
        }
    }

    /**
     * 提取带图标的元素文本
     */
    private void extractElementWithIcon(Document doc, String selector, String key, Map<String, String> map) {
        Element element = doc.selectFirst(selector);
        if (element != null) {
            map.put(key, element.text());
        }
    }

    /**
     * 提取公司工商信息
     */
    private void extractBusinessInfo(Document doc, Map<String, String> companyInfo) {
        for (Element element : doc.select(".level-list li")) {
            Element spanElement = element.selectFirst("span");
            if (spanElement != null) {
                String key = spanElement.text();
                String value = element.text().replace(key, "").trim();

                switch (key) {
                    case "公司名称":
                        companyInfo.put("legalCompanyName", value);
                        if (companyInfo.get("companyName") == null || companyInfo.get("companyName").isEmpty()) {
                            companyInfo.put("companyName", value);
                        }
                        break;
                    case "法定代表人":
                        companyInfo.put("legalRepresentative", value);
                        break;
                    case "成立日期":
                        companyInfo.put("establishDate", value);
                        break;
                    case "企业类型":
                        companyInfo.put("companyType", value);
                        break;
                    case "经营状态":
                        companyInfo.put("operationStatus", value);
                        break;
                    case "注册资金":
                        companyInfo.put("registeredCapital", value);
                        break;
                }
            }
        }
    }

    /**
     * 清除HTML标签，保留纯文本内容
     */
    private String cleanHtmlTags(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        try {
            return html.replaceAll("<br>|<br/>|<br />", "\n")
                    .replaceAll("<[^>]*>", "")
                    .replaceAll("&nbsp;", " ")
                    .replaceAll("\\s+", " ")
                    .replaceAll("&lt;", "<")
                    .replaceAll("&gt;", ">")
                    .replaceAll("&amp;", "&")
                    .trim();
        } catch (Exception e) {
            return html;
        }
    }

    /**
     * 将空字符串或默认值转换为null
     */
    private String convertEmptyToNull(String value) {
        if (value == null || value.trim().isEmpty() || "不限".equals(value) || "无".equals(value) || "暂无".equals(value)) {
            return null;
        }
        return value;
    }

    /**
     * 分析提取的数据，生成职业分析报告
     */
    public JobDetailData generateJobAnalysisReport(JobDetailData jobData) {
        if (jobData == null) {
            throw new IllegalArgumentException("职位数据不能为空");
        }

        try {
            JobAnalysisReport report = new JobAnalysisReport();

            // 薪资分析
            BasicJobInfo basicInfo = jobData.getBasicInfo();
            if (basicInfo != null) {
                report.setSalaryAnalysis(analyzeSalary(basicInfo.getSalary()));
            }

            // 技能需求分析
            JobDescriptionInfo jobDescription = jobData.getJobDescription();
            if (jobDescription != null) {
                report.setSkillRequirements(analyzeSkillRequirements(jobDescription.getRequirements()));
            }

            // 行业分析
            CompanyInfo companyInfo = jobData.getCompanyInfo();
            if (companyInfo != null) {
                report.setIndustryAnalysis(analyzeIndustry(companyInfo.getIndustry()));
            }

            // 职位福利分析
            List<String> jobTags = jobData.getJobTags();
            if (jobTags != null && !jobTags.isEmpty()) {
                report.setBenefitsAnalysis(analyzeBenefits(jobTags));
            }

            jobData.setAnalysisReport(report);
        } catch (Exception e) {
            log.error("生成职业分析报告时发生错误: {}", e.getMessage(), e);
        }

        return jobData;
    }

    /**
     * 分析薪资水平
     */
    private SalaryAnalysis analyzeSalary(String salary) {
        SalaryAnalysis analysis = new SalaryAnalysis();
        if (salary != null && !salary.isEmpty()) {
            try {
                String[] range = salary.replaceAll("[^0-9\\-\\.]", "").split("-");
                if (range.length == 2) {
                    double min = Double.parseDouble(range[0]);
                    double max = Double.parseDouble(range[1]);
                    double avg = (min + max) / 2;
                    analysis.setMinSalary(min);
                    analysis.setMaxSalary(max);
                    analysis.setAvgSalary(avg);
                    analysis.setEstimatedAnnualSalary(avg * 13);
                    analysis.setSalaryLevel(determineSalaryLevel(avg));
                }
            } catch (Exception e) {
                log.error("薪资分析错误: {}", e.getMessage());
            }
        }
        return analysis;
    }

    /**
     * 确定薪资水平级别
     */
    private String determineSalaryLevel(double avgSalary) {
        if (avgSalary > 30)
            return "高薪";
        if (avgSalary > 15)
            return "中高薪";
        if (avgSalary > 8)
            return "中等薪资";
        return "基础薪资";
    }

    /**
     * 分析技能需求
     */
    private SkillRequirements analyzeSkillRequirements(String requirements) {
        SkillRequirements analysis = new SkillRequirements();
        if (requirements != null && !requirements.isEmpty()) {
            // 分析学历要求
            if (requirements.contains("硕士") || requirements.contains("研究生")) {
                analysis.setEducationLevel("硕士及以上");
            } else if (requirements.contains("本科")) {
                analysis.setEducationLevel("本科");
            } else if (requirements.contains("大专")) {
                analysis.setEducationLevel("大专");
            } else {
                analysis.setEducationLevel("不明确");
            }

            // 分析经验要求
            if (requirements.contains("年以上经验") || requirements.contains("年工作经验")) {
                String expText = requirements.replaceAll(".*?(\\d+)年(以上经验|工作经验).*", "$1");
                try {
                    int years = Integer.parseInt(expText);
                    analysis.setExperienceYears(years);
                    if (years >= 5) {
                        analysis.setExperienceLevel("资深");
                    } else if (years >= 3) {
                        analysis.setExperienceLevel("有经验");
                    } else {
                        analysis.setExperienceLevel("初级");
                    }
                } catch (NumberFormatException e) {
                    analysis.setExperienceLevel("有经验要求");
                }
            } else if (requirements.contains("经验不限") || requirements.contains("无经验要求")) {
                analysis.setExperienceLevel("经验不限");
            } else {
                analysis.setExperienceLevel("不明确");
            }

            // 提取关键技能
            List<String> keySkills = new ArrayList<>();
            String[] commonSkills = { "研究", "分析", "报告", "政策", "调研", "战略", "行业", "金融", "数据", "沟通" };
            for (String skill : commonSkills) {
                if (requirements.contains(skill)) {
                    keySkills.add(skill);
                }
            }
            analysis.setKeySkills(keySkills);
        }
        return analysis;
    }

    /**
     * 分析行业信息
     */
    private IndustryAnalysis analyzeIndustry(String industry) {
        IndustryAnalysis analysis = new IndustryAnalysis();
        analysis.setIndustry(industry);

        if (industry != null) {
            if (industry.contains("半导体") || industry.contains("芯片")) {
                analysis.setOutlook("战略新兴产业，国家重点支持发展方向");
                analysis.setGrowthRate("高速增长");
            } else if (industry.contains("互联网") || industry.contains("IT")) {
                analysis.setOutlook("成熟行业，持续发展");
                analysis.setGrowthRate("稳定增长");
            } else if (industry.contains("金融") || industry.contains("投资")) {
                analysis.setOutlook("传统支柱行业，稳定发展");
                analysis.setGrowthRate("稳定");
            } else {
                analysis.setOutlook("需要进一步分析");
                analysis.setGrowthRate("未知");
            }
        }
        return analysis;
    }

    /**
     * 分析职位福利
     */
    private BenefitsAnalysis analyzeBenefits(List<String> benefits) {
        BenefitsAnalysis analysis = new BenefitsAnalysis();

        if (benefits != null && !benefits.isEmpty()) {
            analysis.setBenefitsCount(benefits.size());
            analysis.setAllBenefits(benefits);

            // 分类福利
            List<String> insuranceRelated = new ArrayList<>();
            List<String> bonusRelated = new ArrayList<>();
            List<String> workLifeBalance = new ArrayList<>();
            List<String> developmentRelated = new ArrayList<>();
            List<String> facilitiesRelated = new ArrayList<>();

            for (String benefit : benefits) {
                if (benefit.contains("险") || benefit.contains("金") || benefit.contains("医疗")) {
                    insuranceRelated.add(benefit);
                } else if (benefit.contains("奖") || benefit.contains("分红") || benefit.contains("股")) {
                    bonusRelated.add(benefit);
                } else if (benefit.contains("假") || benefit.contains("调休") || benefit.contains("弹性")) {
                    workLifeBalance.add(benefit);
                } else if (benefit.contains("培训") || benefit.contains("晋升") || benefit.contains("发展")) {
                    developmentRelated.add(benefit);
                } else if (benefit.contains("食") || benefit.contains("宿") || benefit.contains("车")
                        || benefit.contains("网")) {
                    facilitiesRelated.add(benefit);
                }
            }

            analysis.setInsuranceBenefits(insuranceRelated);
            analysis.setBonusBenefits(bonusRelated);
            analysis.setWorkLifeBalanceBenefits(workLifeBalance);
            analysis.setDevelopmentBenefits(developmentRelated);
            analysis.setFacilitiesBenefits(facilitiesRelated);

            // 福利完整度评价
            if (benefits.size() >= 8) {
                analysis.setBenefitsRating("优秀");
            } else if (benefits.size() >= 5) {
                analysis.setBenefitsRating("良好");
            } else if (benefits.size() >= 3) {
                analysis.setBenefitsRating("一般");
            } else {
                analysis.setBenefitsRating("基础");
            }
        }
        return analysis;
    }
}