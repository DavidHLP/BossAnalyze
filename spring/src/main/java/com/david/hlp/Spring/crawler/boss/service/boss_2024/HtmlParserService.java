package com.david.hlp.Spring.crawler.boss.service.boss_2024;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import com.david.hlp.Spring.crawler.boss.model.BasicJobInfo;
import com.david.hlp.Spring.crawler.boss.model.BenefitsAnalysis;
import com.david.hlp.Spring.crawler.boss.model.CompanyInfo;
import com.david.hlp.Spring.crawler.boss.model.IndustryAnalysis;
import com.david.hlp.Spring.crawler.boss.model.JobAnalysisReport;
import com.david.hlp.Spring.crawler.boss.model.JobDescriptionInfo;
import com.david.hlp.Spring.crawler.boss.model.JobDetailData;
import com.david.hlp.Spring.crawler.boss.model.SalaryAnalysis;
import com.david.hlp.Spring.crawler.boss.model.SkillRequirements;

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

    /**
     * 检查HTML内容是否包含IP限制信息
     *
     * @param htmlContent HTML内容字符串
     * @throws IpBlockedException 如果检测到IP被限制
     */
    private void checkIpBlocked(String htmlContent) throws IpBlockedException {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return;
        }

        String ipBlockMessage = "当前IP地址可能存在异常访问行为，完成验证后即可正常使用.";
        if (htmlContent.contains(ipBlockMessage)) {
            log.warn("检测到IP被限制访问：{}", ipBlockMessage);
            throw new IpBlockedException("IP访问受限，需要完成验证：" + ipBlockMessage);
        }
    }

    /**
     * 解析职位详情HTML字符串
     *
     * @param htmlContent HTML内容字符串
     * @return 包含职位信息的JobDetailData对象
     * @throws IOException 解析异常
     */
    public JobDetailData parseJobDetail(String htmlContent) throws IOException {
        log.info("开始解析职位详情HTML");
        if (htmlContent == null || htmlContent.isEmpty()) {
            log.error("HTML内容为空，无法解析");
            throw new IllegalArgumentException("HTML内容不能为空");
        }

        try {
            // 检查IP是否被限制
            checkIpBlocked(htmlContent);

            Map<String, Object> result = new HashMap<>();

            Document doc = Jsoup.parse(htmlContent);
            log.debug("HTML解析为Document对象成功");

            // 提取更新时间
            Element updateTimeElement = doc.selectFirst(".gray");
            if (updateTimeElement != null) {
                String updateTimeText = updateTimeElement.text();
                String updateTime = updateTimeText.replace("页面更新时间：", "").trim();
                result.put("updateTime", updateTime);
                log.debug("更新时间解析完成: {}", updateTime);
            } else {
                log.warn("未找到更新时间元素");
            }

            // 1. 解析基本职位信息
            Map<String, String> basicInfo = parseBasicInfo(doc);
            result.put("basicInfo", basicInfo);
            log.debug("基本职位信息解析完成");

            // 2. 解析职位描述和要求
            Map<String, Object> jobDescription = parseJobDescription(doc);
            result.put("jobDescription", jobDescription);
            log.debug("职位描述和要求解析完成");

            // 3. 解析公司信息
            Map<String, String> companyInfo = parseCompanyInfo(doc);
            result.put("companyInfo", companyInfo);
            log.debug("公司信息解析完成");

            // 4. 解析职位福利标签
            List<String> tags = parseJobTags(doc);
            result.put("jobTags", tags);
            log.debug("职位福利标签解析完成");

            // 5. 清洗数据
            cleanData(result);
            log.debug("数据清洗完成");

            // 6. 转换为JobDetailData对象
            JobDetailData jobDetailData = convertToJobDetailData(result);
            log.info("职位详情HTML解析完成");
            return jobDetailData;
        } catch (Exception e) {
            log.error("解析职位详情HTML时发生错误: {}", e.getMessage(), e);
            throw new IOException("解析职位详情失败", e);
        }
    }

    /**
     * 解析基本职位信息
     */
    private Map<String, String> parseBasicInfo(Document doc) {
        log.debug("开始解析基本职位信息");
        Map<String, String> basicInfo = new HashMap<>();

        try {
            // 职位名称
            Element jobNameElement = doc.selectFirst(".job-primary .name h1");
            if (jobNameElement != null) {
                basicInfo.put("positionName", jobNameElement.text());
            } else {
                log.warn("未找到职位名称元素");
            }

            // 薪资范围
            Element salaryElement = doc.selectFirst(".job-primary .name .salary");
            if (salaryElement != null) {
                basicInfo.put("salary", salaryElement.text());
            } else {
                log.warn("未找到薪资范围元素");
            }

            // 工作城市
            Element cityElement = doc.selectFirst(".text-city");
            if (cityElement != null) {
                basicInfo.put("city", cityElement.text());
            } else {
                log.warn("未找到工作城市元素");
            }

            // 工作经验要求
            Element experienceElement = doc.selectFirst(".text-experiece");
            if (experienceElement != null) {
                basicInfo.put("experience", experienceElement.text());
            } else {
                log.warn("未找到工作经验要求元素");
            }

            // 学历要求
            Element degreeElement = doc.selectFirst(".text-degree");
            if (degreeElement != null) {
                basicInfo.put("degree", degreeElement.text());
            } else {
                log.warn("未找到学历要求元素");
            }

            // 工作地址
            Element addressElement = doc.selectFirst(".location-address");
            if (addressElement != null) {
                basicInfo.put("address", addressElement.text());
            } else {
                log.warn("未找到工作地址元素");
            }
        } catch (Exception e) {
            log.error("解析基本职位信息时发生错误: {}", e.getMessage(), e);
        }

        log.debug("基本职位信息解析结果: {}", basicInfo);
        return basicInfo;
    }

    /**
     * 解析职位描述和要求
     */
    private Map<String, Object> parseJobDescription(Document doc) {
        log.debug("开始解析职位描述和要求");
        Map<String, Object> jobDescription = new HashMap<>();

        try {
            // 获取职位描述文本
            Element descElement = doc.selectFirst(".job-sec-text");
            if (descElement != null) {
                String fullText = descElement.html();

                // 分析职位描述文本，提取任职要求和岗位职责
                if (fullText.contains("任职要求")) {
                    String[] parts = fullText.split("岗位职责");

                    if (parts.length > 0) {
                        String requirements = parts[0].replace("任职要求：", "").trim();
                        jobDescription.put("requirements", requirements);
                    }

                    if (parts.length > 1) {
                        String responsibilities = parts[1].trim();
                        jobDescription.put("responsibilities", responsibilities);
                    }
                } else {
                    jobDescription.put("fullDescription", fullText);
                    log.debug("未找到标准的任职要求和岗位职责分隔，使用完整描述");
                }
            } else {
                log.warn("未找到职位描述元素");
            }

            // 关键词标签
            Elements keywordElements = doc.select(".job-keyword-list li");
            List<String> keywords = new ArrayList<>();
            for (Element element : keywordElements) {
                keywords.add(element.text());
            }
            jobDescription.put("keywords", keywords);
        } catch (Exception e) {
            log.error("解析职位描述和要求时发生错误: {}", e.getMessage(), e);
        }

        @SuppressWarnings("unchecked")
        List<String> savedKeywords = (List<String>)jobDescription.getOrDefault("keywords", new ArrayList<>());
        log.debug("职位描述和要求解析结果: 包含关键词{}个", savedKeywords.size());
        return jobDescription;
    }

    /**
     * 解析公司信息
     */
    private Map<String, String> parseCompanyInfo(Document doc) {
        log.debug("开始解析公司信息");
        Map<String, String> companyInfo = new HashMap<>();

        try {
            // 公司名称
            Element companyNameElement = doc.selectFirst(".sider-company .company-info a");
            if (companyNameElement != null) {
                companyInfo.put("companyName", companyNameElement.text());
            } else {
                // 尝试从其他位置获取公司名称
                Element altCompanyNameElement = doc.selectFirst(".job-detail-company .level-list .company-name");
                if (altCompanyNameElement != null) {
                    String companyName = altCompanyNameElement.text().replace("公司名称", "").trim();
                    companyInfo.put("companyName", companyName);
                } else {
                    // 再尝试从职位标题区域获取
                    Element titleCompanyElement = doc.selectFirst(".company-info");
                    if (titleCompanyElement != null) {
                        companyInfo.put("companyName", titleCompanyElement.text().trim());
                    } else {
                        log.warn("未能从任何位置找到公司名称");
                    }
                }
            }

            // 公司融资阶段
            Element financeElement = doc.selectFirst(".sider-company p:has(.icon-stage)");
            if (financeElement != null) {
                companyInfo.put("financingStage", financeElement.text());
            }

            // 公司规模
            Element scaleElement = doc.selectFirst(".sider-company p:has(.icon-scale)");
            if (scaleElement != null) {
                companyInfo.put("companySize", scaleElement.text());
            }

            // 公司所属行业
            Element industryElement = doc.selectFirst(".sider-company p:has(.icon-industry) a");
            if (industryElement != null) {
                companyInfo.put("industry", industryElement.text());
            }

            // 公司介绍
            Element introElement = doc.selectFirst(".job-detail-company .fold-text");
            if (introElement != null) {
                companyInfo.put("companyIntro", introElement.text());
            }

            // 工商信息
            Elements businessInfoElements = doc.select(".level-list li");
            for (Element element : businessInfoElements) {
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
                        default:
                            log.debug("未处理的公司信息字段: {}", key);
                            break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析公司信息时发生错误: {}", e.getMessage(), e);
        }

        log.debug("公司信息解析结果: {}", companyInfo.get("companyName"));
        return companyInfo;
    }

    /**
     * 解析职位福利标签
     */
    private List<String> parseJobTags(Document doc) {
        log.debug("开始解析职位福利标签");
        List<String> tags = new ArrayList<>();

        try {
            // 只获取第一个出现的标签组，避免重复
            Elements tagElements = doc.select(".job-tags");
            if (!tagElements.isEmpty()) {
                Element firstTagElement = tagElements.first();
                if (firstTagElement != null) {
                    Elements spanElements = firstTagElement.select("span");
                    for (Element element : spanElements) {
                        tags.add(element.text());
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析职位福利标签时发生错误: {}", e.getMessage(), e);
        }

        log.debug("职位福利标签解析结果: {}个标签", tags.size());
        return tags;
    }

    /**
     * 清洗解析得到的数据
     *
     * @param data 解析得到的原始数据
     */
    private void cleanData(Map<String, Object> data) {
        log.debug("开始清洗解析数据");
        try {
            // 1. 清洗职位描述中的HTML标签
            Object jobDescObj = data.get("jobDescription");
            if (jobDescObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> jobDescription = (Map<String, Object>) jobDescObj;
                if (jobDescription.containsKey("requirements")) {
                    String requirements = (String) jobDescription.get("requirements");
                    jobDescription.put("requirements", cleanHtmlTags(requirements));
                }

                if (jobDescription.containsKey("responsibilities")) {
                    String responsibilities = (String) jobDescription.get("responsibilities");
                    jobDescription.put("responsibilities", cleanHtmlTags(responsibilities));
                }

                if (jobDescription.containsKey("fullDescription")) {
                    String fullDescription = (String) jobDescription.get("fullDescription");
                    jobDescription.put("fullDescription", cleanHtmlTags(fullDescription));
                }
            }

            // 2. 去除职位福利标签的重复
            Object jobTagsObj = data.get("jobTags");
            if (jobTagsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> jobTags = (List<String>) jobTagsObj;
                if (!jobTags.isEmpty()) {
                    int beforeSize = jobTags.size();
                    data.put("jobTags", new ArrayList<>(new LinkedHashSet<>(jobTags)));
                    @SuppressWarnings("unchecked")
                    List<String> deduplicatedTags = (List<String>) data.get("jobTags");
                    int afterSize = deduplicatedTags.size();
                    if (beforeSize != afterSize) {
                        log.debug("移除了{}个重复的职位福利标签", beforeSize - afterSize);
                    }
                }
            }

            // 3. 确保公司名称不为空
            Object companyInfoObj = data.get("companyInfo");
            if (companyInfoObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> companyInfo = (Map<String, String>) companyInfoObj;
                if (companyInfo.get("companyName") == null || companyInfo.get("companyName").isEmpty()) {
                    if (companyInfo.containsKey("legalCompanyName") && companyInfo.get("legalCompanyName") != null) {
                        companyInfo.put("companyName", companyInfo.get("legalCompanyName"));
                        log.debug("使用法定公司名称替代空的公司名称");
                    } else {
                        log.warn("公司名称为空且无法从法定名称获取");
                    }
                }
            }
        } catch (Exception e) {
            log.error("清洗数据时发生错误: {}", e.getMessage(), e);
            log.error("清洗数据结构为: {}", data);
        }
    }

    /**
     * 清除HTML标签，保留纯文本内容
     *
     * @param html 包含HTML标签的文本
     * @return 纯文本内容
     */
    private String cleanHtmlTags(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        try {
            // 替换常见HTML标签
            String text = html.replaceAll("<br>|<br/>|<br />", "\n")
                              .replaceAll("<[^>]*>", "")
                              .replaceAll("&nbsp;", " ")
                              .replaceAll("\\s+", " ")
                              .trim();

            // 处理可能的转义字符
            text = text.replaceAll("&lt;", "<")
                       .replaceAll("&gt;", ">")
                       .replaceAll("&amp;", "&");

            return text;
        } catch (Exception e) {
            log.error("清除HTML标签时发生错误: {}", e.getMessage(), e);
            return html; // 返回原始文本作为备选
        }
    }

    /**
     * 将Map格式的数据转换为JobDetailData对象
     *
     * @param data 解析得到的Map数据
     * @return JobDetailData对象
     */
    private JobDetailData convertToJobDetailData(Map<String, Object> data) {
        log.debug("开始将Map数据转换为JobDetailData对象");
        JobDetailData jobDetailData = new JobDetailData();
        try {
            // 设置更新时间
            String updateTime = (String) data.get("updateTime");
            if (updateTime != null) {
                jobDetailData.setUpdateTime(convertEmptyToNull(updateTime));
                log.debug("设置更新时间: {}", updateTime);
            }

            // 转换基本信息
            Object basicInfoObj = data.get("basicInfo");
            if (basicInfoObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> basicInfoMap = (Map<String, String>) basicInfoObj;
                BasicJobInfo basicInfo = new BasicJobInfo();
                basicInfo.setPositionName(convertEmptyToNull(basicInfoMap.get("positionName")));
                basicInfo.setSalary(convertEmptyToNull(basicInfoMap.get("salary")));
                basicInfo.setCity(convertEmptyToNull(basicInfoMap.get("city")));
                basicInfo.setExperience(convertEmptyToNull(basicInfoMap.get("experience")));
                basicInfo.setDegree(convertEmptyToNull(basicInfoMap.get("degree")));
                basicInfo.setAddress(convertEmptyToNull(basicInfoMap.get("address")));
                jobDetailData.setBasicInfo(basicInfo);
                log.debug("基本信息转换完成: 职位名称={}", basicInfo.getPositionName());
            } else {
                log.warn("基本信息为空，无法转换");
            }

            // 转换职位描述
            Object jobDescObj = data.get("jobDescription");
            if (jobDescObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> jobDescMap = (Map<String, Object>) jobDescObj;
                if (jobDescMap != null) {
                    JobDescriptionInfo jobDesc = new JobDescriptionInfo();
                    jobDesc.setRequirements(convertEmptyToNull((String) jobDescMap.get("requirements")));
                    jobDesc.setResponsibilities(convertEmptyToNull((String) jobDescMap.get("responsibilities")));
                    jobDesc.setFullDescription(convertEmptyToNull((String) jobDescMap.get("fullDescription")));
                    Object keywordsObj = jobDescMap.get("keywords");
                    if (keywordsObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> keywords = (List<String>) keywordsObj;
                        jobDesc.setKeywords(keywords.isEmpty() ? null : keywords);
                    }
                    jobDetailData.setJobDescription(jobDesc);
                    log.debug("职位描述转换完成");
                }
            }

            // 转换公司信息
            Object companyInfoObj = data.get("companyInfo");
            if (companyInfoObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> companyInfoMap = (Map<String, String>) companyInfoObj;
                if (companyInfoMap != null) {
                    CompanyInfo companyInfo = new CompanyInfo();
                    companyInfo.setCompanyName(convertEmptyToNull(companyInfoMap.get("companyName")));
                    companyInfo.setFinancingStage(convertEmptyToNull(companyInfoMap.get("financingStage")));
                    companyInfo.setCompanySize(convertEmptyToNull(companyInfoMap.get("companySize")));
                    companyInfo.setIndustry(convertEmptyToNull(companyInfoMap.get("industry")));
                    companyInfo.setCompanyIntro(convertEmptyToNull(companyInfoMap.get("companyIntro")));
                    companyInfo.setLegalCompanyName(convertEmptyToNull(companyInfoMap.get("legalCompanyName")));
                    companyInfo.setLegalRepresentative(convertEmptyToNull(companyInfoMap.get("legalRepresentative")));
                    companyInfo.setEstablishDate(convertEmptyToNull(companyInfoMap.get("establishDate")));
                    companyInfo.setCompanyType(convertEmptyToNull(companyInfoMap.get("companyType")));
                    companyInfo.setOperationStatus(convertEmptyToNull(companyInfoMap.get("operationStatus")));
                    companyInfo.setRegisteredCapital(convertEmptyToNull(companyInfoMap.get("registeredCapital")));
                    jobDetailData.setCompanyInfo(companyInfo);
                    log.debug("公司信息转换完成: 公司名称={}", companyInfo.getCompanyName());
                }
            }

            // 设置职位标签
            Object jobTagsObj = data.get("jobTags");
            if (jobTagsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> jobTags = (List<String>) jobTagsObj;
                if (jobTags != null) {
                    jobDetailData.setJobTags(jobTags.isEmpty() ? null : jobTags);
                    log.debug("职位标签设置完成: {}个标签", jobTags.size());
                }
            }
        } catch (Exception e) {
            log.error("转换JobDetailData对象时发生错误: {}", e.getMessage(), e);
        }
        return jobDetailData;
    }

    /**
     * 将空字符串或默认值转换为null
     *
     * @param value 需要转换的字符串
     * @return 转换后的结果，如果是空字符串或默认值则返回null
     */
    private String convertEmptyToNull(String value) {
        if (value == null || value.trim().isEmpty() || "不限".equals(value) || "无".equals(value) || "暂无".equals(value)) {
            return null;
        }
        return value;
    }

    /**
     * 分析提取的数据，生成职业分析报告
     *
     * @param jobData 解析得到的职位数据
     * @return 包含职业分析报告的JobDetailData对象
     */
    public JobDetailData generateJobAnalysisReport(JobDetailData jobData) {
        log.info("开始生成职业分析报告");
        if (jobData == null) {
            log.error("职位数据为空，无法生成分析报告");
            throw new IllegalArgumentException("职位数据不能为空");
        }

        try {
            JobAnalysisReport report = new JobAnalysisReport();
            // 1. 薪资分析
            BasicJobInfo basicInfo = jobData.getBasicInfo();
            if (basicInfo != null) {
                String salary = basicInfo.getSalary();
                report.setSalaryAnalysis(analyzeSalary(salary));
                log.debug("薪资分析完成: {}", salary);
            } else {
                log.warn("基本信息为空，跳过薪资分析");
            }
            // 2. 技能需求分析
            JobDescriptionInfo jobDescription = jobData.getJobDescription();
            if (jobDescription != null) {
                String requirements = jobDescription.getRequirements();
                report.setSkillRequirements(analyzeSkillRequirements(requirements));
                log.debug("技能需求分析完成");
            } else {
                log.warn("职位描述为空，跳过技能需求分析");
            }
            // 3. 行业分析
            CompanyInfo companyInfo = jobData.getCompanyInfo();
            if (companyInfo != null) {
                String industry = companyInfo.getIndustry();
                report.setIndustryAnalysis(analyzeIndustry(industry));
                log.debug("行业分析完成: {}", industry);
            } else {
                log.warn("公司信息为空，跳过行业分析");
            }
            // 4. 职位福利分析
            List<String> jobTags = jobData.getJobTags();
            if (jobTags != null && !jobTags.isEmpty()) {
                report.setBenefitsAnalysis(analyzeBenefits(jobTags));
                log.debug("职位福利分析完成: {}个福利", jobTags.size());
            } else {
                log.warn("职位福利标签为空，跳过福利分析");
            }
            // 设置分析报告
            jobData.setAnalysisReport(report);
            log.info("职业分析报告生成完成");
        } catch (Exception e) {
            log.error("生成职业分析报告时发生错误: {}", e.getMessage(), e);
        }
        return jobData;
    }

    /**
     * 分析薪资水平
     */
    private SalaryAnalysis analyzeSalary(String salary) {
        log.debug("开始分析薪资水平: {}", salary);
        SalaryAnalysis analysis = new SalaryAnalysis();
        if (salary != null && !salary.isEmpty()) {
            try {
                // 解析薪资范围，例如：12-20K
                String[] range = salary.replaceAll("[^0-9\\-\\.]", "").split("-");
                if (range.length == 2) {
                    try {
                        double min = Double.parseDouble(range[0]);
                        double max = Double.parseDouble(range[1]);
                        double avg = (min + max) / 2;
                        analysis.setMinSalary(min);
                        analysis.setMaxSalary(max);
                        analysis.setAvgSalary(avg);
                        // 按月薪计算年薪（13薪）
                        analysis.setEstimatedAnnualSalary(avg * 13);
                        // 薪资水平分析
                        if (avg > 30) {
                            analysis.setSalaryLevel("高薪");
                        } else if (avg > 15) {
                            analysis.setSalaryLevel("中高薪");
                        } else if (avg > 8) {
                            analysis.setSalaryLevel("中等薪资");
                        } else {
                            analysis.setSalaryLevel("基础薪资");
                        }
                        log.debug("薪资解析结果: 最低{}K, 最高{}K, 平均{}K, 级别: {}", 
                                min, max, avg, analysis.getSalaryLevel());
                    } catch (NumberFormatException e) {
                        log.error("薪资数值解析错误: {}", e.getMessage(), e);
                    }
                } else {
                    log.warn("薪资格式不符合预期: {}", salary);
                }
            } catch (Exception e) {
                log.error("薪资分析时发生错误: {}", e.getMessage(), e);
            }
        } else {
            log.warn("薪资信息为空");
        }
        return analysis;
    }
    /**
     * 分析技能需求
     */
    private SkillRequirements analyzeSkillRequirements(String requirements) {
        log.debug("开始分析技能需求");
        SkillRequirements analysis = new SkillRequirements();
        if (requirements != null && !requirements.isEmpty()) {
            try {
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
                log.debug("学历要求分析结果: {}", analysis.getEducationLevel());
                // 分析经验要求
                if (requirements.contains("年以上经验") || requirements.contains("年工作经验")) {
                    // 提取工作经验年限
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
                        log.debug("工作经验要求: {}年, 级别: {}", years, analysis.getExperienceLevel());
                    } catch (NumberFormatException e) {
                        log.error("经验年限解析错误: {}", e.getMessage(), e);
                        analysis.setExperienceLevel("有经验要求");
                    }
                } else if (requirements.contains("经验不限") || requirements.contains("无经验要求")) {
                    analysis.setExperienceLevel("经验不限");
                } else {
                    analysis.setExperienceLevel("不明确");
                }
                // 提取关键技能
                List<String> keySkills = new ArrayList<>();
                // 这里可以根据不同行业的职位扩展更多关键词匹配
                String[] commonSkills = {"研究", "分析", "报告", "政策", "调研", "战略", "行业", "金融", "数据", "沟通"};
                for (String skill : commonSkills) {
                    if (requirements.contains(skill)) {
                        keySkills.add(skill);
                    }
                }
                analysis.setKeySkills(keySkills);
                log.debug("关键技能匹配结果: {}个技能", keySkills.size());
            } catch (Exception e) {
                log.error("分析技能需求时发生错误: {}", e.getMessage(), e);
            }
        } else {
            log.warn("需求描述为空，无法分析技能需求");
        }

        return analysis;
    }

    /**
     * 分析行业信息
     */
    private IndustryAnalysis analyzeIndustry(String industry) {
        log.debug("开始分析行业信息: {}", industry);
        IndustryAnalysis analysis = new IndustryAnalysis();

        try {
            analysis.setIndustry(industry);

            // 根据行业分类添加行业发展前景分析
            // 这里可以扩展为连接外部API或数据库获取行业数据
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
                log.debug("行业分析结果: 前景={}, 增长率={}", analysis.getOutlook(), analysis.getGrowthRate());
            } else {
                log.warn("行业信息为空");
            }
        } catch (Exception e) {
            log.error("分析行业信息时发生错误: {}", e.getMessage(), e);
        }

        return analysis;
    }

    /**
     * 分析职位福利
     */
    private BenefitsAnalysis analyzeBenefits(List<String> benefits) {
        log.debug("开始分析职位福利: {}个福利", benefits != null ? benefits.size() : 0);
        BenefitsAnalysis analysis = new BenefitsAnalysis();

        if (benefits != null && !benefits.isEmpty()) {
            try {
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
                    } else if (benefit.contains("食") || benefit.contains("宿") || benefit.contains("车") || benefit.contains("网")) {
                        facilitiesRelated.add(benefit);
                    }
                }
                analysis.setInsuranceBenefits(insuranceRelated);
                analysis.setBonusBenefits(bonusRelated);
                analysis.setWorkLifeBalanceBenefits(workLifeBalance);
                analysis.setDevelopmentBenefits(developmentRelated);
                analysis.setFacilitiesBenefits(facilitiesRelated);
                log.debug("福利分类结果: 保险类={}, 奖金类={}, 平衡类={}, 发展类={}, 设施类={}",
                        insuranceRelated.size(), bonusRelated.size(), workLifeBalance.size(),
                        developmentRelated.size(), facilitiesRelated.size());
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
                log.debug("福利完整度评价: {}", analysis.getBenefitsRating());
            } catch (Exception e) {
                log.error("分析职位福利时发生错误: {}", e.getMessage(), e);
            }
        } else {
            log.warn("职位福利为空，无法进行分析");
        }
        return analysis;
    }
}