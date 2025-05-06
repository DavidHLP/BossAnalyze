# BossAnalyze - BOSS 直聘数据分析系统

## 📋 目录

- [📌 项目概述](#-项目概述)
- [🏗️ 系统架构](#-系统架构)
- [🔄 数据流程](#-数据流程)
- [💾 数据模型设计](#-数据模型设计)
- [🔧 技术栈详解](#-技术栈详解)
- [📊 核心功能模块](#-核心功能模块)
- [📈 数据分析能力](#-数据分析能力)
- [🎯 应用场景](#-应用场景)
- [🚀 系统部署](#-系统部署)
- [🔮 未来规划](#-未来规划)
- [👥 贡献指南](#-贡献指南)
- [📜 许可证](#-许可证)
- [📞 联系方式](#-联系方式)

## 📚 在线文档

### 快速访问

🔗 [完整文档](https://deepwiki.com/DavidHLP/BossAnalyze)

### 文档概览

> 📖 **文档包含以下主要内容：**
> - 详细的系统架构说明
> - 完整的API接口文档
> - 部署和配置指南
> - 常见问题解答
> - 开发者指南

💡 **提示：** 建议访问在线文档以获取最新的文档内容和完整的交互体验。

## 📌 项目概述

BossAnalyze 是一个全面的 BOSS 直聘数据分析系统，旨在收集、处理和可视化 BOSS 直聘平台上的招聘数据，为求职者和 HR 提供数据洞察和决策支持。系统采用分布式架构，集成了爬虫、数据处理、数据分析和可视化展示功能，帮助用户了解不同城市、不同职位的薪资水平、职位需求、公司分布等关键信息。

### ✨ 主要特点

| 特点                  | 描述                                                 |
| --------------------- | ---------------------------------------------------- |
| 🔍 **智能数据采集**   | 自动爬取 BOSS 直聘招聘信息，支持多线程并发和反爬策略 |
| 🧹 **高效数据处理**   | 大规模数据清洗和分析，基于 Spark 的分布式处理        |
| 📊 **丰富数据可视化** | 提供薪资热力图、职位需求趋势图等多种可视化展示       |
| 🔐 **完善用户管理**   | 基于 RBAC 的权限控制，保障数据安全                   |
| ⚡ **快速测试原型**   | Python 模块支持快速原型验证和即时分析                |

### 👥 目标用户

- **求职者**：了解市场薪资水平和职位需求
- **企业 HR**：制定合理的薪资策略和招聘计划
- **教育培训机构**：优化课程设置和职业规划
- **政策研究机构**：为政策制定提供数据支持

## 🏗️ 系统架构

```mermaid
graph TD
    classDef crawler fill:#f9d4d4,stroke:#f06292
    classDef storage fill:#d4f9d4,stroke:#66bb6a
    classDef process fill:#d4d4f9,stroke:#5c6bc0
    classDef application fill:#f9f9d4,stroke:#ffee58
    classDef user fill:#f9d4f9,stroke:#ba68c8

    A[爬虫模块] -->|收集原始数据| B[数据存储]
    B -->|原始数据| C[Spark数据处理]
    B -->|数据查询| D[Spring Boot后端]
    C -->|处理结果| B
    D -->|API接口| E[Vue前端]
    F[Python分析] -->|数据分析| B
    F -->|分析结果| B
    E -->|用户交互| G[用户]

    subgraph 数据采集层
    A
    end

    subgraph 数据存储层
    B
    end

    subgraph 数据处理层
    C
    F
    end

    subgraph 应用层
    D
    E
    end

    subgraph 展示层
    G
    end

    class A crawler
    class B storage
    class C,F process
    class D,E application
    class G user
```

### 📦 核心模块

| 模块            | 功能描述                                                                                                            |
| --------------- | ------------------------------------------------------------------------------------------------------------------- |
| 🕸️ **爬虫模块** | 负责从 BOSS 直聘网站爬取招聘数据，包括职位信息、薪资、公司详情等                                                    |
| 💾 **数据存储** | 基于 MySQL 的数据存储系统，分为 Boss 数据库和系统管理数据库                                                         |
| 🔄 **数据处理** | • Spark 模块：处理大规模数据，进行数据清洗、转换和分析<br>• Python 快速测试模块：提供快速数据分析能力，用于原型验证 |
| 🌐 **后端 API** | 基于 Spring Boot 的后端服务，提供 RESTful API 接口                                                                  |
| 📱 **前端界面** | 基于 Vue.js 的现代化响应式前端，展示数据分析结果和可视化图表                                                        |

## 🔄 数据流程

```mermaid
sequenceDiagram
    participant Crawler as 🕸️ 爬虫模块
    participant DB as 💾 数据库
    participant Spark as 🔄 Spark处理
    participant Python as 🐍 Python分析
    participant Backend as 🌐 后端API
    participant Frontend as 📱 前端界面
    participant User as 👤 用户

    Crawler->>DB: 爬取原始HTML数据
    Note over Crawler,DB: 多线程并发爬取
    Crawler->>DB: 解析职位列表和详情
    DB->>Spark: 获取原始数据
    Spark->>Spark: 数据清洗与转换
    Note over Spark: 处理缺失值和异常值
    Spark->>DB: 存储处理后数据
    Python->>DB: 读取职位和城市数据
    Python->>Python: 统计分析
    Note over Python: 快速原型验证
    Python->>DB: 存储分析结果
    User->>Frontend: 访问系统
    Frontend->>Backend: API请求
    Backend->>DB: 查询数据
    DB->>Backend: 返回数据
    Backend->>Frontend: 格式化数据
    Frontend->>User: 可视化展示
```

## 💾 数据模型设计

### 📊 BOSS 数据库模型

```mermaid
erDiagram
    %% 使用更鲜明的颜色和样式
    city_data ||--o{ t_job_detail : 包含
    position_data ||--o{ t_job_detail : 包含
    html_data ||--o{ t_job_detail : 来源
    industry_data ||--o{ position_data : 关联
    job_list ||--o{ t_job_detail : 关联

    city_data {
        int id PK "主键"
        varchar name "城市名称"
        int code "城市代码"
        varchar url "城市URL路径"
    }

    position_data {
        int id PK "主键"
        varchar parent_id "父级职位代码"
        varchar code "职位唯一编码"
        varchar name "职位名称"
        varchar type "职位类型"
    }

    industry_data {
        int id PK "主键"
        varchar parent_id "父级职位代码"
        varchar code "职位唯一编码"
        varchar name "职位名称"
        varchar type "职位类型"
    }

    html_data {
        int id PK "主键"
        varchar url "网页的唯一地址"
        longtext html_content "HTML内容"
        datetime created_at "记录创建时间"
        datetime updated_at "记录更新时间"
        varchar base_city "城市名称"
        varchar base_position "职位名称"
        varchar base_city_code "城市代码"
        varchar base_position_code "职位代码"
        tinyint status "状态标识"
    }

    job_list {
        int id PK "主键"
        varchar html_url "HTML页面URL"
        json json_data "存储JSON格式的数据"
        timestamp created_at "创建时间"
        timestamp updated_at "更新时间"
    }

    t_job_detail {
        bigint id PK "主键"
        varchar position_id "职位唯一标识"
        varchar position_name "职位名称"
        varchar city_id "城市唯一标识"
        varchar city_name "城市名称"
        json detail_data "JobDetailData完整数据"
        datetime gmt_create "创建时间"
        datetime gmt_modified "修改时间"
        tinyint is_deleted "是否删除"
        varchar html_url "html_data表中的url内容"
        text employee_benefits "员工福利"
        text job_requirements "职位需求"
    }

    degree {
        int id PK "主键"
        varchar unique_id "唯一标识"
        varchar city "城市名称"
        varchar degree "学位"
        varchar salary "薪资"
        varchar experience "经验"
        varchar update_time "更新时间"
    }
```

### 🔐 系统管理数据库模型

```mermaid
erDiagram
    %% 使用分组和颜色标记不同模块
    user ||--o{ token : 拥有
    user ||--o{ user_role : 拥有
    role ||--o{ user_role : 被分配
    role ||--o{ role_permission : 拥有
    permission ||--o{ role_permission : 被分配
    permission ||--o{ router : 关联
    router ||--o{ router : 父子关系

    user {
        bigint id PK "主键"
        varchar name "用户名"
        varchar avatar "头像"
        varchar introduction "介绍"
        varchar email "邮箱"
        tinyint status "用户状态"
        varchar address "地址"
        varchar last_login_ip "最后登录IP"
        datetime last_login "最后登录时间"
        bigint role_id "角色ID"
        datetime create_time "创建时间"
        varchar password "加密密码"
    }

    token {
        bigint id PK "主键"
        bigint user_id FK "用户ID"
        varchar token "Token值"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
        varchar token_type "令牌类型"
        tinyint expired "是否过期"
        tinyint revoked "是否撤销"
    }

    role {
        bigint id PK "主键"
        varchar role_name "角色名称"
        tinyint status "状态"
        varchar remark "备注"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
    }

    permission {
        bigint id PK "主键"
        tinyint status "状态"
        varchar remark "备注"
        varchar permission "权限标识"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
    }

    role_permission {
        bigint role_id PK,FK "角色ID"
        bigint permission_id PK,FK "权限ID"
    }

    user_role {
        bigint user_id PK,FK "用户ID"
        bigint role_id PK,FK "角色ID"
    }

    router {
        bigint id PK "主键"
        bigint pid FK "父路由ID"
        int menu_order "菜单顺序"
        tinyint status "状态"
        varchar remark "备注"
        varchar permission "权限标识"
        varchar path "路由路径"
        varchar name "路由名称"
        enum type "类型"
        varchar component "组件路径"
        varchar redirect "重定向路径"
        tinyint always_show "总是显示"
        varchar meta_title "元标题"
        varchar meta_icon "元图标"
        tinyint meta_hidden "元隐藏"
        json meta_roles "元角色"
        tinyint meta_keep_alive "元保持活跃"
        tinyint hidden "是否隐藏"
        varchar icon "图标"
        bigint role_id "角色ID"
    }

    nginx_access_log {
        bigint id PK "主键"
        varchar ip "IP地址"
        varchar access_time "访问时间"
        varchar method "HTTP方法"
        varchar path "访问路径"
        varchar protocol "HTTP协议版本"
        varchar status "HTTP状态码"
        varchar bytes "响应大小"
        text referrer "引用来源"
        text user_agent "用户代理"
        varchar user_id "用户ID"
        timestamp created_at "记录创建时间"
    }
```

## 🔧 技术栈详解

### 🌈 前端技术栈

```mermaid
graph LR
    classDef framework fill:#ff9e80,stroke:#ff6d00,color:#000
    classDef language fill:#80d8ff,stroke:#0288d1,color:#000
    classDef tools fill:#ccff90,stroke:#64dd17,color:#000
    classDef ui fill:#ea80fc,stroke:#aa00ff,color:#000
    classDef data fill:#b388ff,stroke:#7c4dff,color:#000

    A[Vue.js] -->|框架| B[前端应用]
    C[TypeScript] -->|语言| B
    D[Vite] -->|构建工具| B
    E[Vue Router] -->|路由| B
    F[Pinia] -->|状态管理| B
    G[Element Plus] -->|UI组件库| B
    H[ECharts] -->|数据可视化| B
    I[Axios] -->|HTTP客户端| B

    class A,E,F framework
    class C language
    class D,I tools
    class G ui
    class H data
```

### 🏢 后端技术栈

```mermaid
graph LR
    classDef framework fill:#ff9e80,stroke:#ff6d00,color:#000
    classDef database fill:#80d8ff,stroke:#0288d1,color:#000
    classDef tools fill:#ccff90,stroke:#64dd17,color:#000
    classDef security fill:#ea80fc,stroke:#aa00ff,color:#000
    classDef language fill:#b388ff,stroke:#7c4dff,color:#000

    A[Spring Boot] -->|框架| B[后端服务]
    C[MyBatis] -->|ORM框架| B
    D[MySQL] -->|数据库| B
    E[Redis] -->|缓存| B
    F[Spring Security] -->|安全框架| B
    G[JWT] -->|认证| B
    H[Java] -->|语言| B

    class A,C,F framework
    class D,E database
    class G tools
    class F security
    class H language
```

### 🕸️ 爬虫技术栈

```mermaid
graph LR
    classDef language fill:#b388ff,stroke:#7c4dff,color:#000
    classDef tools fill:#ccff90,stroke:#64dd17,color:#000
    classDef framework fill:#ff9e80,stroke:#ff6d00,color:#000
    classDef database fill:#80d8ff,stroke:#0288d1,color:#000
    classDef concurrent fill:#ea80fc,stroke:#aa00ff,color:#000

    A[Java] -->|语言| B[爬虫服务]
    C[HttpClient] -->|HTTP客户端| B
    D[Jsoup] -->|HTML解析| B
    E[Spring Boot] -->|框架| B
    F[MySQL] -->|数据存储| B
    G[多线程] -->|并发处理| B

    class A language
    class C,D tools
    class E framework
    class F database
    class G concurrent
```

### 📊 数据处理技术栈

```mermaid
graph LR
    classDef bigdata fill:#ff9e80,stroke:#ff6d00,color:#000
    classDef language fill:#80d8ff,stroke:#0288d1,color:#000
    classDef tools fill:#ccff90,stroke:#64dd17,color:#000
    classDef analysis fill:#ea80fc,stroke:#aa00ff,color:#000
    classDef db fill:#b388ff,stroke:#7c4dff,color:#000

    A[Apache Spark] -->|大数据处理| B[数据处理服务]
    C[Python] -->|快速测试| D[数据分析]
    E[Pandas] -->|数据分析库| D
    F[NumPy] -->|科学计算| D
    G[MySQL Connector] -->|数据库连接| D
    H[Scala] -->|语言| B
    I[Java] -->|语言| B

    class A bigdata
    class C,H,I language
    class G tools
    class E,F analysis
    class G db
```

## 📊 核心功能模块

### 🕸️ 数据采集模块

数据采集模块主要负责从 BOSS 直聘网站爬取招聘信息，包括但不限于以下数据：

- 职位基本信息：职位名称、薪资范围、工作地点
- 职位详细要求：学历要求、经验要求、技能要求
- 公司信息：公司名称、公司规模、融资阶段、所属行业

爬虫采用分布式架构，支持多线程并发爬取，同时实现了 IP 代理池、请求频率控制等反爬机制，确保数据采集的稳定性和可靠性。

### 🔄 数据处理模块

数据处理模块基于 Apache Spark 和 Python 实现，主要功能包括：

#### Spark 大数据处理

- **数据清洗**：处理缺失值、异常值，标准化数据格式
- **数据转换**：将非结构化数据转换为结构化数据，如薪资范围转换为数值
- **数据聚合**：按城市、职位、行业等维度聚合数据
- **统计分析**：计算平均薪资、职位分布、技能需求热度等指标

#### Python 快速测试模块

- **城市数据分析**：获取城市列表、统计城市工作数量
- **职位薪资分析**：计算不同职位的薪资范围和推荐城市
- **二维数据分析**：将薪资、学历、经验等数据进行转换，用于图表展示
- **快速统计报表**：生成即时分析结果，支持快速原型验证

### 📈 数据可视化模块

数据可视化模块基于 Vue.js 和 ECharts 实现，提供丰富的图表展示：

| 图表类型              | 展示内容                     | 应用场景                 |
| --------------------- | ---------------------------- | ------------------------ |
| 🌡️ **薪资热力图**     | 不同城市、不同职位的薪资水平 | 求职者了解各地区薪资情况 |
| 📈 **职位需求趋势图** | 职位需求随时间的变化趋势     | HR 分析人才市场变化      |
| 🎯 **技能需求雷达图** | 不同职位所需技能的分布情况   | 求职者了解技能提升方向   |
| 🗺️ **公司分布地图**   | 招聘公司的地理分布情况       | 求职者了解就业机会分布   |

### 👤 用户管理模块

用户管理模块基于 Spring Security 和 JWT 实现，提供安全的用户认证和授权机制：

- **用户注册与登录**：支持邮箱注册、密码加密存储
- **角色权限管理**：基于 RBAC 模型实现细粒度的权限控制
- **操作日志记录**：记录用户的关键操作，支持审计追踪

## 📈 数据分析能力

BossAnalyze 系统提供以下核心数据分析能力：

### 薪资分析

```mermaid
graph TD
    A[原始薪资数据] -->|数据清洗| B[标准化薪资数据]
    B -->|城市维度分析| C[城市薪资排名]
    B -->|职位维度分析| D[职位薪资排名]
    B -->|经验维度分析| E[经验薪资关系]
    B -->|学历维度分析| F[学历薪资关系]
    B -->|行业维度分析| G[行业薪资排名]
    B -->|公司规模分析| H[公司规模薪资关系]
```

### 职位需求分析

```mermaid
graph TD
    A[职位需求数据] -->|文本分析| B[关键词提取]
    B -->|技能需求分析| C[热门技能排名]
    B -->|经验要求分析| D[经验要求分布]
    B -->|学历要求分析| E[学历要求分布]
    A -->|地域分析| F[地域需求热度]
    A -->|行业分析| G[行业需求分布]
```

### 趋势预测分析

```mermaid
graph TD
    A[历史数据] -->|时间序列分析| B[趋势识别]
    B -->|薪资趋势| C[薪资涨幅预测]
    B -->|需求趋势| D[职位需求预测]
    B -->|技能趋势| E[技能热度预测]
    A -->|相关性分析| F[影响因素识别]
    F -->|回归模型| G[薪资影响因素]
```

## 🎯 应用场景

1. **求职者决策支持**：

   - 了解不同城市、不同职位的薪资水平
   - 掌握不同职位的技能需求和经验要求
   - 识别高薪职位和热门职位，优化求职策略

2. **企业 HR 参考**：

   - 了解市场薪资水平，制定合理的薪资策略
   - 分析人才需求趋势，优化招聘计划
   - 评估竞争对手的招聘情况，制定差异化策略

3. **教育培训机构**：

   - 了解市场技能需求，优化课程设置
   - 分析不同行业的人才需求，提供精准的职业规划建议
   - 跟踪技能需求变化趋势，预测未来热门技能

4. **政策研究机构**：
   - 分析区域就业情况，为政策制定提供数据支持
   - 研究产业结构变化对就业市场的影响
   - 评估教育投入与就业市场的匹配程度

## 🚀 系统部署

### 开发环境

- JDK 17+
- Node.js 16+
- Python 3.8+
- MySQL 8.0+
- Redis 6.0+
- Spark 3.3.0

### 部署架构

```mermaid
graph TD
    A[用户] -->|访问| B[Nginx]
    B -->|静态资源| C[Vue前端]
    B -->|API请求| D[Spring Boot后端]
    D -->|读写数据| E[MySQL数据库]
    D -->|缓存| F[Redis]
    G[爬虫服务] -->|写入数据| E
    H[Spark集群] -->|处理数据| E
    I[Python快速测试模块] -->|分析数据| E
```

## 🔮 未来规划

1. **数据源扩展**：接入更多招聘平台数据，如拉勾、智联招聘等
2. **AI 能力增强**：引入机器学习模型，提供个性化职业规划建议
3. **实时数据分析**：支持实时数据采集和分析，提供最新市场动态
4. **国际化支持**：扩展支持国际招聘市场数据分析
5. **移动端应用**：开发移动端应用，提供随时随地的数据查询服务

## 👥 贡献指南

欢迎对 BossAnalyze 项目进行贡献，您可以通过以下方式参与：

1. 提交 Issue：报告 bug 或提出新功能建议
2. 提交 Pull Request：贡献代码或文档改进
3. 参与讨论：在 Issue 或 Discussion 中分享您的想法

## 📜 许可证

本项目采用 MIT 许可证，详情请参阅 LICENSE 文件。

## 📞 联系方式

- 项目负责人：David
- 项目仓库：https://github.com/DavidHLP/BossAnalyze
- 电子邮件：lysf15520112973@163.com
