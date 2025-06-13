# BossAnalyze - 职位分析系统

## 1. 技术架构

### 1.1 整体架构图

```mermaid
graph TD
    A[用户] --> B[SpringBoot Web层]
    B --> C[业务服务层]
    C --> D[Spark分析引擎]
    D --> E[(MySQL数据库)]
    C --> F[(Redis缓存)]

    subgraph "前端"
    A
    end

    subgraph "后端应用"
    B
    C
    end

    subgraph "数据处理"
    D
    end

    subgraph "存储层"
    E
    F
    end
```

### 1.2 技术栈选型

#### 后端技术栈

- **核心框架**：Spring Boot 2.x
- **数据分析引擎**：Apache Spark
- **缓存**：Redis
- **数据库**：MySQL
- **构建工具**：Maven

#### 数据处理技术栈

- **分布式计算**：Apache Spark Core、Spark SQL
- **流处理**：Spark Streaming
- **数据连接器**：Spark SQL Kafka

#### 开发工具与环境

- **JDK 版本**：JDK 8
- **项目管理**：Maven
- **API 测试**：Postman
- **版本控制**：Git

### 1.3 系统模块划分

```mermaid
graph LR
    A[SpringSpark主应用] --> B[用户分析模块]
    A --> C[公司分析模块]
    A --> D[日志分析模块]

    B --> B1[薪资热门职位分析]
    B --> B2[二维职位分析]
    B --> B3[基础数据集管理]

    C --> C1[公司相关分析]

    D --> D1[系统日志分析]

    subgraph "核心业务模块"
    B
    C
    end

    subgraph "基础设施模块"
    D
    end
```

## 2. 核心技术实现

### 2.1 关键功能技术方案

#### 2.1.1 Spark 与 Spring Boot 集成

系统通过自定义配置类（SparkLocalConfig/SparkClusterConfig）将 Spark 引擎与 Spring Boot 框架无缝集成，提供：

- 本地模式开发支持
- 集群模式部署能力
- 依赖注入式 Spark 组件调用

```java
@Configuration
public class SparkLocalConfig {
    @Bean
    public SparkSession sparkSession() {
        return SparkSession
                .builder()
                .config(sparkConf())
                .getOrCreate();
    }
}
```

#### 2.1.2 高薪职位分析

系统利用 Spark SQL 进行复杂数据分析，支持：

- 基于大数据集的职位薪资统计
- 城市维度的职位推荐
- 多维度交叉分析

#### 2.1.3 二维职位分析图表

通过 Spark 对职位数据进行多维度分析，实现：

- 薪资与学历的相关性分析
- 城市与职位的交叉分析
- 自定义维度的数据查询

### 2.2 数据处理流程

```mermaid
flowchart TD
    A[MySQL原始数据] --> B[Spark数据加载]
    B --> C[数据清洗与转换]
    C --> D[Spark SQL分析]
    D --> E[结果缓存至Redis]
    E --> F[API数据返回]

    G[用户查询请求] --> H{缓存是否存在?}
    H -- 是 --> E
    H -- 否 --> B
```

1. **数据采集与存储**：职位数据存储在 MySQL 数据库中
2. **数据加载**：通过 Spark JDBC 连接器从 MySQL 加载数据
3. **数据转换**：使用 Spark DataFrame API 进行数据清洗和结构转换
4. **数据分析**：应用 Spark SQL 进行复杂查询和聚合计算
5. **结果缓存**：分析结果存入 Redis 以提高查询性能
6. **数据展示**：通过 REST API 将结果返回给前端

### 2.3 性能优化策略

#### 2.3.1 Spark 引擎优化

- 数据集缓存：对频繁使用的 DataFrame 执行 cache()操作
- 分区优化：根据数据规模自动调整分区数量
- 内存管理：合理配置 Spark executor 内存

#### 2.3.2 查询性能优化

- Redis 缓存：热点数据缓存，减少重复计算
- 延迟加载：按需启动 Spark 分析任务
- 结果预计算：定时任务预计算常用分析结果

#### 2.3.3 系统配置优化

- 连接池管理：Redis 和 MySQL 连接池优化
- 日志分级：针对性配置不同级别的日志输出
- 资源隔离：为 Spark 任务分配独立资源

## 3. 部署与运行

### 3.1 环境要求

- JDK 8+
- Maven 3.6+
- Redis 6.0+
- MySQL 5.7+