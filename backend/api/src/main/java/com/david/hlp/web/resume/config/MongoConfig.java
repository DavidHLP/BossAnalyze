package com.david.hlp.web.resume.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * MongoDB配置类，启用审计功能以支持自动填充创建时间和更新时间
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
    // 启用MongoDB审计功能
}