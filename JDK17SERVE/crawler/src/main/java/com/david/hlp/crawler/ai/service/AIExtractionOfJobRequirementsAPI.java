package com.david.hlp.crawler.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIExtractionOfJobRequirementsAPI {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openrouter.api.key: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}")
    private String apiKey;

    @Value("${openrouter.api.url:https://openrouter.ai/api/v1/chat/completions}")
    private String apiUrl;

    @Value("${openrouter.api.model:deepseek/deepseek-r1-0528-qwen3-8b:free}")
    private String model;

    /**
     * 创建请求头
     * 
     * @return HttpHeaders
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("HTTP-Referer", "https://boss.analyze.tool");
        headers.set("X-Title", "Boss Analyze Tool");
        headers.set("Content-Type", "application/json");
        return headers;
    }

    /**
     * 创建请求体
     * 
     * @param content 用户消息内容
     * @return 请求体对象
     */
    private ChatRequest createRequestBody(String content) {
        Message message = new Message("user", content);
        ChatRequest request = new ChatRequest(model, Collections.singletonList(message));
        request.setResponse_format(new ResponseFormat("json"));
        return request;
    }

    /**
     * 调用 OpenRouter API
     * 
     * @param prompt 提示内容
     * @return API 响应内容
     */
    private String callOpenRouterApi(String prompt) {
        try {
            // 创建请求头和请求体
            HttpHeaders headers = createHeaders();
            ChatRequest requestBody = createRequestBody(prompt);

            // 创建请求实体
            HttpEntity<ChatRequest> requestEntity = new HttpEntity<>(requestBody, headers);

            // 发送 POST 请求
            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl,
                    requestEntity,
                    String.class);

            // 解析响应
            String responseBody = response.getBody();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode choicesNode = rootNode.path("choices");

            if (choicesNode.isArray() && choicesNode.size() > 0) {
                JsonNode messageNode = choicesNode.get(0).path("message");
                if (!messageNode.isMissingNode()) {
                    return messageNode.path("content").asText();
                }
            }

            log.error("无法从 API 响应中提取内容: {}", responseBody);
            return "";
        } catch (Exception e) {
            log.error("调用 OpenRouter API 时发生错误", e);
            return "";
        }
    }

    /**
     * 解析 JSON 数组字符串为字符串列表
     * 
     * @param jsonArrayString JSON 数组字符串
     * @return 字符串列表
     */
    private List<String> parseJsonArrayToList(String jsonArrayString) {
        try {
            // 尝试直接解析为字符串数组
            return objectMapper.readValue(jsonArrayString, List.class);
        } catch (JsonProcessingException e) {
            log.error("解析 JSON 数组失败，尝试提取 JSON 部分", e);

            // 尝试从文本中提取 JSON 数组部分
            int startIndex = jsonArrayString.indexOf('[');
            int endIndex = jsonArrayString.lastIndexOf(']') + 1;

            if (startIndex >= 0 && endIndex > startIndex) {
                String extractedJson = jsonArrayString.substring(startIndex, endIndex);
                try {
                    return objectMapper.readValue(extractedJson, List.class);
                } catch (JsonProcessingException ex) {
                    log.error("从提取的文本中解析 JSON 数组失败", ex);
                }
            }

            // 如果无法解析，返回一个包含原始文本的列表
            return Collections.singletonList("无法解析结果: " + jsonArrayString);
        }
    }

    /**
     * 提取职位要求
     * 
     * @param jobDescription 职位描述
     * @return 职位要求列表
     */
    public List<String> extractJobRequirements(String jobDescription) {
        String userMessage = String.format(
                "请解析以下 JSON 格式的职位描述，并提取所有的任职要求。\n" +
                        "**严格**按照下面的示例格式返回一个 JSON 字符串数组。\n" +
                        "**仅**提取任职要求。\n" +
                        "**必须**返回有效的 JSON 数组，即使信息不明确或难以提取，也要尽力提取至少一项要求，或者返回一个表示\"未明确说明\"的条目。**绝对不要**返回空的 JSON 数组 (`[]`)。\n\n"
                        +
                        "示例输出格式 (如果能提取到):\n" +
                        "[\"熟练掌握 Java\", \"了解 Spring Boot\", \"熟悉 MySQL\"]\n\n" +
                        "示例输出格式 (如果未明确说明):\n" +
                        "[\"任职要求未明确说明\"]\n\n" +
                        "职位描述 JSON:\n%s",
                jobDescription);

        String response = callOpenRouterApi(userMessage);
        return parseJsonArrayToList(response);
    }

    /**
     * 提取职位福利
     * 
     * @param jobDescription 职位描述
     * @return 职位福利列表
     */
    public List<String> extractJobBenefits(String jobDescription) {
        String userMessage = String.format(
                "请解析以下 JSON 格式的职位描述，并提取所有的职位福利。\n" +
                        "**严格**按照下面的示例格式返回一个 JSON 字符串数组。\n" +
                        "**仅**提取职位福利。\n" +
                        "**必须**返回有效的 JSON 数组，即使信息不明确或难以提取，也要尽力提取至少一项福利，或者返回一个表示\"未明确说明\"的条目。**绝对不要**返回空的 JSON 数组 (`[]`)。\n\n"
                        +
                        "示例输出格式 (如果能提取到):\n" +
                        "[\"五险一金\", \"带薪年假\", \"年终奖\"]\n\n" +
                        "示例输出格式 (如果未明确说明):\n" +
                        "[\"职位福利未明确说明\"]\n\n" +
                        "职位描述 JSON:\n%s",
                jobDescription);

        String response = callOpenRouterApi(userMessage);
        return parseJsonArrayToList(response);
    }

    /**
     * 分析核心要求
     * 
     * @param jobDescription 职位描述
     * @return 核心要求列表
     */
    public List<String> coreRequirementAnalyzer(String jobDescription) {
        String userMessage = String.format(
                "请分析以下职位描述，提取最具代表性的5-20个核心工作要求。\n" +
                        "**严格**按照下面的示例格式返回一个 JSON 字符串数组。\n" +
                        "**仅**提取最核心、最重要的要求。\n" +
                        "**必须**返回有效的 JSON 数组，即使信息不明确或难以提取，也要尽力提取至少一项要求，或者返回一个表示\"未明确说明\"的条目。**绝对不要**返回空的 JSON 数组 (`[]`)。\n\n"
                        +
                        "示例输出格式 (如果能提取到):\n" +
                        "[\"精通Java开发\", \"熟悉Spring Cloud微服务架构\", \"具备5年以上开发经验\"]\n\n" +
                        "示例输出格式 (如果未明确说明):\n" +
                        "[\"核心要求未明确说明\"]\n\n" +
                        "职位描述:\n%s",
                jobDescription);

        String response = callOpenRouterApi(userMessage);
        return parseJsonArrayToList(response);
    }

    /**
     * 获取用户与职位的匹配度
     * 
     * @param jobDescription 职位描述
     * @param resume         简历内容
     * @return 匹配度评分 (0-10)
     */
    public Integer getUserSimilarity(String jobDescription, String resume) {
        String userMessage = String.format(
                "请分析以下职位描述和简历，评估该职位对求职者的推荐程度。\n" +
                        "根据简历中的技能、经验与职位要求的匹配情况,给出一个0到10的职位推荐度评分。\n" +
                        "**必须按以下JSON格式返回结果**:\n" +
                        "{\"answer\": 评分}\n\n" +
                        "评分标准：\n" +
                        "- 0:完全不推荐\n" +
                        "- 5:一般推荐\n" +
                        "- 10:强烈推荐\n\n" +
                        "职位描述:\n%s\n\n" +
                        "简历:\n%s",
                jobDescription, resume);

        String response = callOpenRouterApi(userMessage);
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode answerNode = rootNode.path("answer");
            if (!answerNode.isMissingNode()) {
                return answerNode.asInt(5); // 默认返回5
            }

            // 如果没有找到标准格式，尝试直接解析数字
            return Integer.parseInt(response.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            log.error("解析用户相似度评分失败", e);
            return 5; // 出错时返回中等分数
        }
    }

    /**
     * 用户相似度分析 - 包含城市和职位参数
     * 
     * @param resume   简历
     * @param city     城市
     * @param position 职位
     * @return 用户相似度列表
     */
    public List<Object> getUserSimilarity(String resume, String city, String position) {
        // 根据实际需求实现，这里返回一个示例
        // 此方法应与原 AIExtractionOfJobRequirements 类中的同名方法保持功能一致
        return new ArrayList<>();
    }

    /**
     * 聊天请求体
     */
    @Data
    private static class ChatRequest {
        private String model;
        private List<Message> messages;
        private ResponseFormat response_format;

        public ChatRequest(String model, List<Message> messages) {
            this.model = model;
            this.messages = messages;
        }
    }

    /**
     * 消息对象
     */
    @Data
    private static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    /**
     * 响应格式控制
     */
    @Data
    private static class ResponseFormat {
        private String type;

        public ResponseFormat(String type) {
            this.type = type;
        }
    }

    /**
     * API 响应对象
     */
    @Data
    private static class ApiResponse {
        private List<Choice> choices;

        @Data
        public static class Choice {
            private Message message;
        }
    }
}