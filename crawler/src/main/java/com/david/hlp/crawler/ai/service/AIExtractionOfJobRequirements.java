package com.david.hlp.crawler.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import com.david.hlp.crawler.ai.entity.RequirementsWrapper;
import org.springframework.core.ParameterizedTypeReference;
import java.util.List;
@Service
public class AIExtractionOfJobRequirements {

    private final ChatClient chatClient;

    public AIExtractionOfJobRequirements(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public List<String> extractJobRequirements(String jobDescription) {
        String userMessage = """
                请解析以下 JSON 格式的职位描述，并提取所有的任职要求。
                **严格**按照下面的示例格式返回一个 JSON 字符串数组。
                **仅**提取任职要求。
                **必须**返回有效的 JSON 数组，即使信息不明确或难以提取，也要尽力提取至少一项要求，或者返回一个表示“未明确说明”的条目。**绝对不要**返回空的 JSON 数组 (`[]`)。

                示例输出格式 (如果能提取到):
                ["熟练掌握 Java", "了解 Spring Boot", "熟悉 MySQL"]

                示例输出格式 (如果未明确说明):
                ["任职要求未明确说明"]

                职位描述 JSON:
                %s
                """.formatted(jobDescription);

        // 使用 ChatClient 的 fluent API 并指定返回类型为 List<String>
        RequirementsWrapper wrapper = chatClient.prompt()
                .user(userMessage)
                .call()
                .entity(new ParameterizedTypeReference<RequirementsWrapper>() {});

        // 从包装器中返回列表
        return wrapper.getItems();
    }

    public List<String> extractJobBenefits(String jobDescription) {
        String userMessage = """
                请解析以下 JSON 格式的职位描述，并提取所有的职位福利。
                **严格**按照下面的示例格式返回一个 JSON 字符串数组。
                **仅**提取职位福利。
                **必须**返回有效的 JSON 数组，即使信息不明确或难以提取，也要尽力提取至少一项福利，或者返回一个表示“未明确说明”的条目。**绝对不要**返回空的 JSON 数组 (`[]`)。

                示例输出格式 (如果能提取到):
                ["五险一金", "带薪年假", "年终奖"]

                示例输出格式 (如果未明确说明):
                ["职位福利未明确说明"]

                职位描述 JSON:
                %s
                """.formatted(jobDescription);

        // 使用 ChatClient 的 fluent API 并指定返回类型为 List<String>
        RequirementsWrapper wrapper = chatClient.prompt()
                .user(userMessage)
                .call()
                .entity(new ParameterizedTypeReference<RequirementsWrapper>() {});

        // 从包装器中返回列表
        return wrapper.getItems();
    }
}
