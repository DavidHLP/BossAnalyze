# BossAnalyze系统 - 访问日志分析模块需求文档

## 1. 项目概述

### 1.1 项目背景

BossAnalyze系统是一套针对企业级应用的综合分析平台，其中访问日志分析模块（AccessLog）是系统的核心组件之一。该模块负责收集、处理和分析系统的访问日志，为管理员和运维人员提供实时的系统使用情况、异常检测和性能监控等数据支持。

随着业务规模的扩大，系统每日产生的访问日志数量呈几何级增长，传统的日志分析方式已无法满足快速响应和深度分析的需求。因此，基于大数据技术栈（Hadoop、Spark等）构建高效的日志分析模块成为系统升级的重要任务。

### 1.2 项目目标

- 构建实时高效的日志采集和分析系统，支持大规模日志数据处理
- 提供多维度、多粒度的数据分析能力，满足不同场景的监控需求
- 实现异常访问的自动检测和预警功能，提高系统安全性
- 为管理决策提供数据支持，通过分析用户行为优化系统功能和资源配置
- 支持定制化报表和可视化展示，提升数据价值

## 2. 系统架构

### 2.1 技术栈

- **存储层**：HDFS（日志原始数据存储）、Redis（分析结果缓存）
- **计算层**：Apache Spark（分布式计算框架）
- **应用层**：Spring Boot（后端服务）、Vue3（前端展示）
- **调度层**：Spring Scheduler（定时任务）

### 2.2 系统模块

```
                 ┌─────────────┐
                 │   Web服务器  │
                 └──────┬──────┘
                        │ 产生日志
                        ▼
┌──────────────────────────────────────┐
│              日志收集系统            │
└──────────────┬───────────────────────┘
               │ 存储
               ▼
┌──────────────────────────────────────┐
│                 HDFS                 │
└──────────────┬───────────────────────┘
               │ 读取
               ▼
┌──────────────────────────────────────┐
│           Spark分析引擎              │
└──────────────┬───────────────────────┘
               │ 分析结果
               ▼
┌──────────────────────────────────────┐
│              Redis缓存               │
└──────────────┬───────────────────────┘
               │ 查询
               ▼
┌──────────────────────────────────────┐
│            Web API接口               │
└──────────────┬───────────────────────┘
               │ 数据展示
               ▼
┌──────────────────────────────────────┐
│            前端可视化界面            │
└──────────────────────────────────────┘
```

## 3. 功能需求

### 3.1 日志采集与处理

#### 3.1.1 日志格式

系统需要处理的自定义访问日志格式如下：

```
2025-05-29 10:44:49.746 INFO 45408 --- [http-nio-8080-exec-4] com.david.hlp.web.system.auth.JwtAuthenticationFilter : ACCESS|ts=1748486689746|ip=127.0.0.1|path=/api/auth/demo/getUserPrivateInformation|method=OPTIONS|ua=Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:139.0) Gecko/20100101 Firefox/139.0
```

系统需要过滤基础SpringBoot日志（不包含`ACCESS|`标记的日志行），只处理包含`ACCESS|`标记的自定义访问日志。

#### 3.1.2 日志字段

从访问日志中需要提取的关键字段包括：

- **timestamp**：访问时间戳，格式为`yyyy-MM-dd HH:mm:ss`
- **ip**：客户端IP地址
- **path**：请求路径
- **method**：HTTP请求方法（GET、POST、PUT、DELETE等）
- **user_agent**：用户代理信息

#### 3.1.3 日志预处理

- 数据清洗：过滤无效日志、去除重复记录
- 数据转换：提取字段、标准化格式
- 数据增强：添加衍生字段（日期、小时、分钟、API分类等）

### 3.2 分析指标

#### 3.2.1 基础统计分析

- 总请求数
- 独立IP数量
- 不同API路径数量
- API类别分布统计
- 热门API路径排行

#### 3.2.2 时间维度分析

- **分钟级统计**：每分钟请求量（近24小时数据）
- **小时级统计**：每小时请求量（按天分组）
- **日级统计**：每日请求总量
- **周级分析**：工作日vs周末访问对比
- **月度分析**：月度趋势分析
- **小时峰值分析**：识别业务高峰时段
- **热力图数据**：日期和小时的二维分布

#### 3.2.3 API使用模式分析

- 按日期时间的API访问路径统计
- HTTP方法分布（GET、POST、PUT、DELETE等）
- API关联分析：识别常见的API调用序列

#### 3.2.4 用户行为分析

- 访问频率最高的IP地址
- IP和API路径组合分析
- 用户代理（浏览器、设备类型等）分布

#### 3.2.5 异常检测与安全分析

- 请求量异常监测（基于统计方法）
- 访问模式异常检测
- 潜在安全威胁识别

#### 3.2.6 系统负载分析

- 每小时请求数统计
- 每小时平均请求数
- 峰值分析（每分钟最高请求数）

### 3.3 可视化与报表

#### 3.3.1 实时监控仪表板

- 系统总览：关键指标实时展示
- 实时流量监控：近期请求趋势图
- 异常警报面板：突出显示异常情况

#### 3.3.2 趋势分析报表

- 时间序列分析：小时/日/周/月度趋势
- 对比分析：不同时间段数据对比
- 预测分析：基于历史数据的趋势预测

#### 3.3.3 用户行为分析报表

- 用户画像：基于访问行为的用户分类
- 路径分析：用户访问路径可视化
- 地理分布：访问IP的地理位置分布

#### 3.3.4 异常检测报表

- 异常事件列表与详情
- 异常分类与统计
- 安全威胁评估

## 4. 非功能需求

### 4.1 性能需求

- 系统应能处理每日至少10GB的日志数据
- 批处理分析任务应在1小时内完成
- API查询响应时间应在500ms以内
- 前端页面加载时间应在2秒以内

### 4.2 可靠性需求

- 系统可用性应达到99.9%
- 数据分析结果应准确无误
- 系统应能在服务器重启后自动恢复
- 应具备数据备份和恢复机制

### 4.3 可扩展性需求

- 系统架构应支持水平扩展
- 应能适应日志量增长10倍的情况
- 支持新增分析维度和指标
- 支持自定义报表和可视化需求

### 4.4 安全性需求

- 访问控制：基于角色的权限管理
- 数据安全：敏感信息脱敏处理
- 操作审计：记录系统关键操作
- 合规性：符合相关数据隐私法规

## 5. 实现计划

### 5.1 开发阶段

| 阶段 | 内容 | 时间周期 |
|------|------|--------|
| 需求分析 | 明确业务需求，确定系统功能和技术方案 | 2周 |
| 环境搭建 | 配置Hadoop、Spark等开发环境 | 1周 |
| 核心开发 | 实现日志解析、数据处理、分析算法 | 4周 |
| 前端开发 | 实现可视化界面和交互功能 | 3周 |
| 集成测试 | 系统集成和功能测试 | 2周 |
| 性能优化 | 系统性能调优和压力测试 | 2周 |
| 部署上线 | 系统部署和试运行 | 1周 |

### 5.2 迭代计划

#### 第一阶段：基础功能（MVP）

- 日志采集与存储
- 基础统计分析
- 简单可视化展示

#### 第二阶段：增强功能

- 多维度分析能力
- 异常检测功能
- 完善的可视化报表

#### 第三阶段：高级功能

- 智能预警机制
- 预测分析功能
- 自定义分析和报表

## 6. 日志分析模块详细设计

### 6.1 日志解析模块

```java
private Dataset<Row> loadAndParseLogs(String logsDir) {
    return sparkSession.read()
            .option("mode", "PERMISSIVE")
            .option("columnNameOfCorruptRecord", "_corrupt_record")
            .text(logsDir + "/*.log")
            // 过滤出包含 "ACCESS|" 标记的自定义日志行，忽略基础SpringBoot日志
            .filter(functions.col("value").contains("ACCESS|"))
            .select(
                    functions.regexp_extract(functions.col("value"), "(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})",
                            1).as("timestamp"),
                    functions.regexp_extract(functions.col("value"), "ip=([^|]+)", 1).as("ip"),
                    functions.regexp_extract(functions.col("value"), "path=([^|]+)", 1).as("path"),
                    functions.regexp_extract(functions.col("value"), "method=([^|]+)", 1).as("method"),
                    functions.regexp_extract(functions.col("value"), "ua=([^|]+)", 1).as("user_agent"))
            .withColumn("timestamp", functions.to_timestamp(functions.col("timestamp"), "yyyy-MM-dd HH:mm:ss"))
            .withColumn("date", functions.date_format(functions.col("timestamp"), "yyyy-MM-dd"))
            .withColumn("hour", functions.hour(functions.col("timestamp")))
            .withColumn("minute", functions.minute(functions.col("timestamp")))
            .withColumn("api_category", functions.regexp_extract(functions.col("path"), "/api/boss/([^/]+)", 1));
}
```

### 6.2 时间维度分析模块

```java
private void analyzeTimeBasedStats(Dataset<Row> logs) {
    log.info("开始时间维度细粒度分析...");
    
    // 添加日期时间相关列
    Dataset<Row> logsWithTime = logs
            .withColumn("date_hour", functions.concat(functions.col("date"), 
                    functions.lit(" "), functions.lpad(functions.col("hour").cast("string"), 2, "0")))
            .withColumn("date_hour_minute", functions.concat(functions.col("date_hour"), 
                    functions.lit(":"), functions.lpad(functions.col("minute").cast("string"), 2, "0")))
            .withColumn("weekday", functions.date_format(functions.col("timestamp"), "E"))
            .withColumn("week_of_year", functions.weekofyear(functions.col("timestamp")))
            .withColumn("month", functions.month(functions.col("timestamp")))
            .withColumn("day", functions.dayofmonth(functions.col("timestamp")));
    
    // 各种时间维度统计...
    // 分钟级、小时级、日级、周级、月级统计
    
    // 收集并缓存所有结果
    Map<String, Object> timeStats = new HashMap<>();
    // 添加各种统计结果到Map中
    
    redisCache.setCacheMap(REDIS_TIME_STATS_KEY, timeStats);
    redisCache.expire(REDIS_TIME_STATS_KEY, CACHE_EXPIRE_HOURS * 3600);
}
```

### 6.3 异常检测模块

```java
private void analyzeAnomalies(Dataset<Row> logs) {
    log.info("开始异常检测分析...");

    // 按分钟统计请求数
    Dataset<Row> minuteStats = logs.groupBy("date", "hour", "minute")
            .count()
            .sort(functions.col("date"), functions.col("hour"), functions.col("minute"));

    // 计算统计指标（平均值、标准差、95分位数）
    // 基于统计指标设置阈值
    // 检测超过阈值的异常点
    
    // 缓存异常检测结果
    Map<String, Object> anomalies = new HashMap<>();
    // 添加异常检测结果到Map中
    
    redisCache.setCacheMap(REDIS_ANOMALY_STATS_KEY, anomalies);
    redisCache.expire(REDIS_ANOMALY_STATS_KEY, CACHE_EXPIRE_HOURS * 3600);
}
```

## 7. 接口设计

### 7.1 API接口列表

| 接口路径 | 方法 | 描述 |
|---------|------|------|
| `/api/boss/logs/stats/basic` | GET | 获取基础统计信息 |
| `/api/boss/logs/stats/time` | GET | 获取时间维度统计 |
| `/api/boss/logs/stats/api` | GET | 获取API使用统计 |
| `/api/boss/logs/stats/user` | GET | 获取用户行为统计 |
| `/api/boss/logs/stats/anomaly` | GET | 获取异常检测结果 |
| `/api/boss/logs/stats/load` | GET | 获取系统负载统计 |

### 7.2 数据结构示例

#### 7.2.1 基础统计响应

```json
{
  "totalRequests": 12563,
  "uniqueIPs": 245,
  "uniquePaths": 78,
  "apiCategoryStats": {
    "user": 3456,
    "order": 2891,
    "product": 1789,
    "payment": 967
  },
  "popularPaths": {
    "/api/boss/user/getUserInfo": 1234,
    "/api/boss/order/list": 987,
    "/api/boss/product/detail": 654
  }
}
```

#### 7.2.2 时间维度统计响应

```json
{
  "minuteStats": {
    "2025-05-29 10:44": 145,
    "2025-05-29 10:45": 156,
    "2025-05-29 10:46": 132
  },
  "hourlyStats": {
    "2025-05-29 10": 1543,
    "2025-05-29 11": 1687,
    "2025-05-29 12": 1245
  },
  "dailyStats": {
    "2025-05-28": 15632,
    "2025-05-29": 16745
  },
  "weekdayStats": {
    "Mon": 15234,
    "Tue": 16432,
    "Wed": 17654
  }
}
```

## 8. 前端设计

### 8.1 页面布局

- **Dashboard**：系统总览，关键指标和图表
- **流量分析**：时间维度的访问量分析
- **API分析**：API使用情况分析
- **用户行为**：用户访问行为分析
- **异常监控**：异常事件检测和告警
- **报表中心**：自定义报表和导出功能

### 8.2 可视化组件

- 实时监控面板（仪表盘）
- 折线图（趋势分析）
- 柱状图（分类数据对比）
- 饼图（占比分析）
- 热力图（二维分布）
- 数据表格（详细数据展示）

## 9. 系统维护与扩展

### 9.1 日常维护

- 日志数据定期清理和归档
- 分析任务调度监控
- 系统性能监控和调优
- 数据备份和恢复测试

### 9.2 功能扩展

- 实时流处理引擎集成（Apache Flink）
- 机器学习模型集成（异常检测、预测分析）
- 多数据源整合（结合业务数据进行分析）
- 移动端监控应用开发

## 10. 项目风险与应对策略

| 风险 | 影响 | 应对策略 |
|------|------|--------|
| 日志数据量剧增 | 系统性能下降 | 优化存储结构，实施数据分片和冷热分离 |
| 分析任务耗时过长 | 实时性降低 | 优化Spark任务，考虑增加计算资源 |
| 数据质量问题 | 分析结果不准确 | 加强数据清洗和验证机制 |
| 安全风险 | 数据泄露 | 加强访问控制和敏感信息脱敏 |

## 11. 项目结构与实现细节

### 11.1 项目核心结构

```
BossAnalyze/
├── JDK8SERVE/                  # Spark分析服务（JDK 8环境）
│   └── spark/
│       ├── src/
│       │   └── main/java/com/david/hlp/spark/
│       │       ├── service/
│       │       │   └── Log/
│       │       │       └── AccessLogService.java  # 日志分析Spark任务实现
│       │       └── utils/
│       │           └── RedisCache.java            # Redis缓存工具类
│       └── model/                                # 模型存储目录
│
├── JDK17SERVE/                 # Spring Boot API服务（JDK 17环境）
│   └── spring/
│       └── src/main/java/com/david/hlp/web/
│           ├── log/
│           │   ├── controller/
│           │   │   └── AccessLogController.java   # REST API控制器
│           │   ├── service/
│           │   │   └── AccessLogService.java      # 服务层实现
│           │   └── model/
│           │       ├── AccessLogStats.java        # 数据模型
│           │       ├── AllStats.java
│           │       ├── IpStats.java
│           │       ├── TimeStats.java
│           │       └── UrlStats.java
│           └── common/
│               └── util/
│                   └── RedisCache.java            # Redis工具类
│
└── vue/                        # 前端应用（Vue 3 + TS）
    └── src/
        ├── api/
        │   └── log/
        │       ├── accesslog.d.ts                # API类型定义
        │       └── accesslog.ts                  # API调用接口
        ├── views/
        │   └── console/
        │       └── system/
        │           └── dashboard/
        │               └── DashboardView.vue     # 仪表盘页面
        └── components/
            └── ...                              # 通用组件
```

### 11.2 组件职责与数据流

#### 11.2.1 Spark日志分析服务

**核心组件：** `JDK8SERVE/spark/src/main/java/com/david/hlp/spark/service/Log/AccessLogService.java`

**主要职责：**
- 定时从HDFS读取原始日志文件
- 筛选包含`ACCESS|`标记的自定义日志行，过滤普通SpringBoot日志
- 执行多维度数据分析：基础统计、API模式、用户行为、异常检测、系统负载、时间维度分析等
- 将分析结果存储到Redis缓存中，设置过期时间

**调度机制：**
- 使用`@Scheduled(fixedRate = 60 * 60 * 1000)`注解实现每小时执行一次
- 通过`@PostConstruct`注解确保应用启动时执行一次初始分析

**数据处理流程：**

```
原始日志 → 日志过滤 → 字段提取 → 多维度分析 → 结果缓存 → Redis存储
```

#### 11.2.2 Spring Boot API服务

**核心组件：** 
- `JDK17SERVE/spring/src/main/java/com/david/hlp/web/log/service/AccessLogService.java`
- `JDK17SERVE/spring/src/main/java/com/david/hlp/web/log/controller/AccessLogController.java`

**主要职责：**
- 从Redis缓存中读取Spark分析结果
- 将数据转换为前端需要的格式
- 提供REST API接口供前端调用
- 提供数据验证和转换功能

**API接口：**

| 接口路径 | 方法 | 描述 |
|---------|------|------|
| `/api/v1/system/logs/summary` | GET | 获取访问日志基础统计信息 |
| `/api/v1/system/logs/ip-stats` | GET | 获取IP统计数据 |
| `/api/v1/system/logs/url-stats` | GET | 获取URL统计数据 |
| `/api/v1/system/logs/time-stats` | GET | 获取时间维度统计 |
| `/api/v1/system/logs/all-stats` | GET | 一次性获取所有统计数据 |
| `/api/v1/system/logs/validate` | GET | 验证Redis数据一致性 |

#### 11.2.3 Vue前端应用

**核心组件：**
- `vue/src/api/log/accesslog.ts`：API调用接口
- `vue/src/api/log/accesslog.d.ts`：TypeScript类型定义
- `vue/src/views/console/system/dashboard/DashboardView.vue`：仪表盘页面

**主要职责：**
- 通过API接口获取后端数据
- 使用ECharts库进行数据可视化
- 提供交互式仪表盘展示各类统计信息
- 支持数据刷新和定时更新

**仪表盘内容：**
- 基础指标卡片（总请求数、独立IP数、访问路径数等）
- 访问IP TOP10图表
- 访问URL TOP10图表
- 每小时访问分布图表
- API类别分布图表
- 热门路径分析图表

### 11.3 组件交互流程

整体数据流程如下：

```
┌────────────┐    定时任务     ┌───────────┐    存储结果    ┌─────────┐
│ 原始日志文件 │───────────────>│ Spark分析 │───────────────>│  Redis  │
└────────────┘   每小时执行    └───────────┘               └────┬────┘
                                                             │
                                                             │ 读取数据
                                                             ▼
┌────────────┐    HTTP请求     ┌───────────┐    查询缓存    ┌─────────┐
│  Vue前端   │<───────────────>│ Spring API│<──────────────>│  Redis  │
└────────────┘    REST API     └───────────┘               └─────────┘
```

1. **数据采集与存储**：系统日志被收集并存储到HDFS

2. **批处理分析**：
   - Spark分析服务每小时执行一次分析任务
   - 从HDFS读取日志文件
   - 进行多维度分析
   - 将结果存入Redis，设置24小时过期时间

3. **数据查询与展示**：
   - 用户访问Vue前端应用
   - 前端调用Spring Boot REST API
   - API服务从Redis获取分析结果
   - 前端接收数据并使用ECharts进行可视化展示

4. **实时更新**：
   - 仪表盘页面定时刷新数据
   - 用户可手动触发数据刷新
   - 支持实时监控系统状态

### 11.4 扩展与优化方向

1. **日志分析引擎升级**：
   - 引入实时流处理（如Spark Streaming或Flink）
   - 支持更复杂的分析模型和机器学习算法

2. **数据存储优化**：
   - 引入时序数据库存储历史趋势
   - 实现冷热数据分离，优化查询性能

3. **前端体验提升**：
   - 增加更多自定义可视化组件
   - 支持导出报表和数据下载
   - 提供更丰富的数据筛选和过滤条件

4. **智能分析能力**：
   - 异常行为自动预警
   - 访问模式识别与聚类
   - 预测性分析与趋势预测