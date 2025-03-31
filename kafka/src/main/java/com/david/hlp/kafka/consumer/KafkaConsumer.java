package com.david.hlp.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.david.hlp.kafka.service.BossDataJobsService;
import com.david.hlp.kafka.entity.Degree;
import com.david.hlp.kafka.producer.KafkaProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
/**
 * Kafka消息消费者
 *
 * @author david
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumer {

    private final BossDataJobsService bossDataJobsService;
    private final KafkaProducer kafkaProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String PRODUCER_TOPIC = "analyzer_degree";

    /**
     * 消费示例主题消息
     *
     * @param record 消费者记录
     * @param ack    确认对象
     */
    @KafkaListener(topics = "boss_data_jobs", groupId = "analyzer-degree-group-test")
    public void consumeMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String key = record.key();
        String value = record.value();
        int partition = record.partition();
        long offset = record.offset();

        log.info("接收到消息: topic = {}, partition = {}, offset = {}, key = {}, value = {}",
                record.topic(), partition, offset, key, value);

        try {
            // 处理JSON数据
            Degree degreeInfo = bossDataJobsService.processJobData(value);
            if (degreeInfo != null) {
                log.info("处理后的数据: {}", degreeInfo);
                kafkaProducer.sendMessage(PRODUCER_TOPIC, objectMapper.writeValueAsString(degreeInfo));
            }
            // 手动确认消息已消费
            ack.acknowledge();
            log.info("消息处理成功");
        } catch (Exception e) {
            log.error("消息处理失败: {}", e.getMessage());
            // 异常处理，可以选择不确认或重试
        }
    }
}