package com.david.hlp.kafka.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class KafkaProducer {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 发送消息
     *
     * @param topic   主题
     * @param message 消息内容
     */
    public void sendMessage(String topic, String message) {
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
    
    /**
     * 发送带键的消息（相同的键会被发送到相同的分区）
     *
     * @param topic   主题
     * @param key     键
     * @param message 消息内容
     */
    public void sendMessageWithKey(String topic, String key, String message) {
        log.info("发送消息到主题: {}, 键: {}, 消息内容: {}", topic, key, message);
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, message);
        
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