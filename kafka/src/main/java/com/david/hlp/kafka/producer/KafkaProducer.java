package com.david.hlp.kafka.producer;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka消息生产者
 *
 * @author david
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 发送消息
     *
     * @param topic   主题
     * @param message 消息内容
     */
    public void sendMessage(@NonNull String topic, @NonNull String message) {
        log.info("发送消息到主题: {}, 消息内容: {}", topic, message);
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, message);

        // 添加回调
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("消息发送失败: {}", ex.getMessage());
            } else {
                log.info("消息发送成功: topic = {}, partition = {}, offset = {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}