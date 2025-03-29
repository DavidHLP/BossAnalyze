package com.david.hlp.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka消息消费者
 *
 * @author david
 */
@Slf4j
@Component
public class KafkaConsumer {

    /**
     * 消费示例主题消息
     *
     * @param record 消费者记录
     * @param ack    确认对象
     */
    @KafkaListener(topics = "example-topic", groupId = "example-group")
    public void consumeMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String key = record.key();
        String value = record.value();
        int partition = record.partition();
        long offset = record.offset();
        
        log.info("接收到消息: topic = {}, partition = {}, offset = {}, key = {}, value = {}",
                record.topic(), partition, offset, key, value);
        
        try {
            // 在这里处理消息
            log.info("处理消息: {}", value);
            
            // 手动确认消息已消费
            ack.acknowledge();
        } catch (Exception e) {
            log.error("消息处理失败: {}", e.getMessage());
            // 异常处理，可以选择不确认或重试
        }
    }
    
    /**
     * 消费其他主题的消息（示例）
     *
     * @param record 消费者记录
     * @param ack    确认对象
     */
    @KafkaListener(topics = "another-topic", groupId = "another-group")
    public void consumeAnotherTopic(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("接收到另一个主题的消息: topic = {}, partition = {}, offset = {}, value = {}",
                record.topic(), record.partition(), record.offset(), record.value());
        
        // 处理消息并确认
        ack.acknowledge();
    }
} 