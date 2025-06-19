# Redis Utils - Redis工具类集合

这是一个基于Spring Boot的Redis工具类模块，提供了便捷的Redis操作工具，包括缓存、分布式锁、布隆过滤器等功能。

## 功能特性

- **RedisCacheUtil**: 基础Redis操作工具类，支持String、Hash、List、Set、ZSet等数据类型
- **RedisLockUtil**: 分布式锁工具类，基于Redisson实现
- **BloomFilterUtil**: 布隆过滤器工具类，用于防止缓存穿透
- **RedisMutexUtil**: 互斥锁缓存工具类，防止缓存击穿
- **RedisCacheHelper**: 高级缓存辅助工具类，整合了上述所有功能

## 使用方法

### 1. 安装到本地Maven仓库

在项目根目录下执行：

```bash
cd JDK17SERVE/redis-utils
mvn clean install
```

### 2. 在其他项目中引入依赖

在其他项目的 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.david.hlp</groupId>
    <artifactId>redis-utils</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 3. 配置Redis连接

在其他项目的 `application.yml` 中配置Redis连接：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
    password: # 如果有密码
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

### 4. 在代码中使用

由于配置了自动装配，可以直接注入使用：

```java
@Service
public class YourService {
    
    @Autowired
    private RedisCacheHelper redisCacheHelper;
    
    public void example() {
        // 获取或加载缓存
        String result = redisCacheHelper.getOrLoadString("key", 60, () -> {
            return "从数据库或其他地方加载的数据";
        });
        
        // 使用分布式锁
        redisCacheHelper.executeWithLock("lockKey", () -> {
            // 需要加锁执行的代码
        });
        
        // 设置对象缓存
        User user = new User();
        redisCacheHelper.setObject("user:1", user, User.class, 30, TimeUnit.MINUTES);
        
        // 获取对象缓存
        User cachedUser = redisCacheHelper.getObject("user:1", User.class);
    }
}
```

### 5. 高级用法

```java
@Service
public class AdvancedService {
    
    @Autowired
    private RedisCacheUtil redisCacheUtil;
    
    @Autowired
    private RedisLockUtil redisLockUtil;
    
    @Autowired
    private BloomFilterUtil bloomFilterUtil;
    
    public void advancedExample() {
        // 使用布隆过滤器防止缓存穿透
        bloomFilterUtil.initBloomFilter("user_filter", 100000, 0.01);
        bloomFilterUtil.add("user_filter", "user:1");
        
        if (bloomFilterUtil.mightContain("user_filter", "user:1")) {
            // 可能存在，继续查询缓存或数据库
        } else {
            // 肯定不存在，直接返回null
        }
        
        // 手动使用分布式锁
        boolean locked = redisLockUtil.tryLock("mylock", 10, 30, TimeUnit.SECONDS);
        if (locked) {
            try {
                // 业务逻辑
            } finally {
                redisLockUtil.unlock("mylock");
            }
        }
    }
}
```

## 依赖要求

- JDK 17+
- Spring Boot 3.4.5+
- Spring Data Redis
- Redisson 3.17.6+
- Hutool工具库

## 版本说明

当前版本：0.0.1-SNAPSHOT

## 更新日志

- 0.0.1-SNAPSHOT: 初始版本，包含基础Redis工具类功能 