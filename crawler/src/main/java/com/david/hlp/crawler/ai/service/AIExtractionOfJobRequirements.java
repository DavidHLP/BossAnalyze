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
                **仅**返回有效的 JSON 数组。

                示例输出格式:
                []

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
                **仅**返回有效的 JSON 数组。

                示例输出格式:
                []

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
