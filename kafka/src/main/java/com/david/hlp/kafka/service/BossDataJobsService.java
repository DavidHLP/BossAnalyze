package com.david.hlp.kafka.service;

import com.david.hlp.kafka.mapper.DegreeMapper;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.david.hlp.kafka.entity.Degree;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
@Slf4j
@Service
@RequiredArgsConstructor
public class BossDataJobsService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DegreeMapper degreeMapper;
    public Degree processJobData(String jsonData) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonData);
            // 获取data.detail_data.basicInfo中的字段
            JsonNode dataNode = rootNode.path("data");
            JsonNode detailDataNode = dataNode.path("detail_data");
            JsonNode basicInfoNode = detailDataNode.path("basicInfo");

            // 获取所需字段值
            String uniqueId = dataNode.path("id").asText(null);
            String city = basicInfoNode.path("city").asText(null);
            String degree = basicInfoNode.path("degree").asText(null);
            String salary = basicInfoNode.path("salary").asText(null);
            String experience = basicInfoNode.path("experience").asText(null);

            // 处理updateTime，只保留年份
            String updateTime = detailDataNode.path("updateTime").asText(null);
            String updateYear = null;
            if (updateTime != null && !updateTime.isEmpty() && updateTime.length() >= 4) {
                updateYear = updateTime.substring(0, 4);
            }

            // 验证所有字段是否有效
            if (isNullOrEmpty(city) || isNullOrEmpty(degree) ||
                isNullOrEmpty(salary) || isNullOrEmpty(experience) ||
                isNullOrEmpty(updateYear) || isNullOrEmpty(uniqueId)) {
                log.warn("数据无效，存在空字段: city={}, degree={}, salary={}, experience={}, updateTime={}", 
                        city, degree, salary, experience, updateYear);
                return null;
            }

            // 创建并返回Degree对象
            Degree degreeInfo = Degree.builder()
                .uniqueId(uniqueId)
                .city(city)
                .degree(degree)
                .salary(salary)
                .experience(experience)
                .updateTime(updateYear)
                .build();
            if (degreeMapper.getByUniqueId(uniqueId) == null) {
                // 插入数据库
                degreeMapper.insert(degreeInfo);
            }
            return degreeInfo;
        } catch (Exception e) {
            log.error("JSON处理失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 判断字符串是否为null或空
     *
     * @param str 待检查的字符串
     * @return 是否为null或空
     */
    private boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty() || "null".equals(str);
    }
}
