package com.david.hlp.kafka.controller;

import com.david.hlp.kafka.producer.KafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Kafka消息发送控制器
 *
 * @author david
 */
@RestController
@RequestMapping("/api/kafka")
public class KafkaController {

    @Autowired
    private KafkaProducer kafkaProducer;

    /**
     * 发送消息到指定主题
     *
     * @param topic   主题名称
     * @param message 消息内容
     * @return 操作结果
     */
    @PostMapping("/send")
    public String sendMessage(@RequestParam("topic") String topic, @RequestParam("message") String message) {
        kafkaProducer.sendMessage(topic, message);
        return "消息已发送到主题：" + topic;
    }

    /**
     * 发送带键的消息到指定主题
     *
     * @param topic   主题名称
     * @param key     消息键
     * @param message 消息内容
     * @return 操作结果
     */
    @PostMapping("/send-with-key")
    public String sendMessageWithKey(
            @RequestParam("topic") String topic,
            @RequestParam("key") String key,
            @RequestParam("message") String message) {
        kafkaProducer.sendMessageWithKey(topic, key, message);
        return "消息已发送到主题：" + topic + "，键：" + key;
    }
} 