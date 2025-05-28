# Boss招聘网站数据爬虫

## 项目概述

这是一个基于Spring Boot的Boss直聘网站数据爬虫项目，用于自动化抓取职位信息、公司信息等数据。项目采用分布式架构设计，支持多线程爬取、断点续爬、反反爬虫策略等功能。

## 功能特点

- **多线程爬取**：使用线程池实现高效并发爬取
- **断点续爬**：记录爬取状态，支持异常中断后继续爬取
- **智能反反爬**：实现随机等待、请求间隔、User-Agent轮换等策略
- **数据持久化**：支持MySQL数据存储
- **任务调度**：基于Spring Schedule实现定时任务调度
- **分布式支持**：集成Redis实现分布式锁，支持多实例部署
- **AI集成**：集成Ollama AI模型进行数据处理和分析

## 技术栈

- **核心框架**：Spring Boot 3.x
- **Web驱动**：Selenium WebDriver
- **HTML解析**：JSoup
- **数据库**：MySQL
- **缓存**：Redis
- **任务调度**：Spring Schedule
- **AI集成**：Spring AI + Ollama
- **构建工具**：Maven
- **日志**：SLF4J + Logback

## 快速开始

### 环境要求

- JDK 17+
- MySQL 5.7+
- Redis 5.0+
- Chrome/Chromium浏览器
- Maven 3.6+

### 数据库配置

1. 创建数据库：
```sql
CREATE DATABASE IF NOT EXISTS boss_data CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行SQL脚本初始化表结构（位于`src/main/resources/sql/`目录下）

### 配置说明

修改`application.yml`中的配置项：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/boss_data?useSSL=false&characterEncoding=UTF-8
    username: root
    password: your_password
  redis:
    host: 127.0.0.1
    port: 6379
    password: your_redis_password

# WebDriver配置
webdriver:
  chrome:
    driver: /path/to/chromedriver
    headless: true
    disable-gpu: true
    no-sandbox: true
    disable-dev-shm-usage: true
```

### 启动应用

```bash
mvn spring-boot:run
```

## 项目结构

```
src/main/java/com/david/hlp/crawler/
├── ai/                    # AI相关功能
│   ├── controller/        # API接口
│   ├── entity/           # 实体类
│   ├── executor/         # 执行器
│   ├── lock/             # 分布式锁
│   ├── mapper/           # 数据访问层
│   ├── service/          # 服务层
│   └── task/             # 定时任务
├── boss/                 # Boss直聘爬虫核心
│   ├── config/           # 配置类
│   ├── entity/           # 实体类
│   ├── exception/        # 异常处理
│   ├── mapper/           # 数据访问层
│   ├── model/            # 数据模型
│   └── service/          # 服务层
├── common/               # 公共模块
│   ├── config/           # 公共配置
│   └── threadpool/       # 线程池配置
└── utils/                # 工具类
```

## 定时任务

项目包含以下定时任务：

1. **URL爬取任务**：每分钟执行一次，负责抓取职位列表页URL
   ```java
   @Scheduled(fixedDelay = 1000 * 60)
   public void startScrapeBossUrl()
   ```

2. **HTML解析任务**：每30秒执行一次，解析已抓取的HTML内容
   ```java
   @Scheduled(fixedDelay = 1000 * 30)
   public void startParseBossHtmlData()
   ```

3. **数据抓取任务**：每分钟执行一次，抓取详细的职位数据
   ```java
   @Scheduled(fixedDelay = 1000 * 60)
   public void startScrapeBossData()
   ```

## 反爬策略

项目实现了以下反反爬策略：

1. 随机请求间隔（10-60秒）
2. 随机User-Agent轮换
3. 请求头随机化
4. Cookie管理
5. 验证码处理
6. IP代理池（需自行实现）

## 注意事项

1. 请合理设置爬取频率，避免对目标网站造成过大压力
2. 建议使用代理IP池，防止IP被封禁
3. 定期清理Redis缓存，避免内存占用过高
4. 生产环境建议关闭调试日志，提高性能

## 常见问题

### 1. 如何修改爬取的城市和职位？

修改`CityData`和`PositionData`表中的数据，设置需要爬取的城市和职位信息。

### 2. 如何处理验证码？

项目实现了基本的验证码处理逻辑，但建议：
- 使用打码平台
- 降低爬取频率
- 使用代理IP

### 3. 如何扩展新的数据源？

1. 在`model`包中定义新的数据模型
2. 在`mapper`包中创建对应的Mapper接口
3. 在`service`包中实现业务逻辑
4. 在`controller`包中暴露API接口

## 贡献指南

欢迎提交Issue和Pull Request，共同完善项目。

## 许可证

[MIT License](LICENSE)
