#!/bin/bash

#######################################################################
# 大数据组件单机模式初始化脚本
# 
# 功能说明：
# 1. 初始化Hadoop单机模式
# 2. 初始化HBase单机目录
# 3. 初始化Spark单机环境
#
# 使用方法：bash env-init.sh
# 作者：DavidHLP
# 版本：1.2
#######################################################################

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
ORANGE='\033[0;33m'
NC='\033[0m' # No Color
HADOOP_ENV_DIR="$HOME/opt/stand-alone/hadoop-spark-hive-hbase/hadoop/etc/hadoop"
HBASE_ENV_DIR="$HOME/opt/stand-alone/hadoop-spark-hive-hbase/hbase/conf"
SPARK_ENV_DIR="$HOME/opt/stand-alone/hadoop-spark-hive-hbase/spark/conf"

# 配置Hadoop的core-site.xml和hdfs-site.xml
configure_hadoop_xml() {
    log_info "配置Hadoop的XML配置文件..."
    
    # 配置core-site.xml
    local core_site_file="${HADOOP_ENV_DIR}/core-site.xml"
    log_info "配置core-site.xml: $core_site_file"
    
    cat > "$core_site_file" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
    <property>
        <name>fs.defaultFS</name>
        <value>hdfs://${DOCKER_CONTAINER}:9000</value>
    </property>
    <property>
        <name>hadoop.tmp.dir</name>
        <value>/opt/hadoop/data</value>
    </property>
</configuration>
EOF
    
    if [ $? -ne 0 ]; then
        log_error "配置core-site.xml失败"
        return 1
    fi
    
    # 配置hdfs-site.xml
    local hdfs_site_file="${HADOOP_ENV_DIR}/hdfs-site.xml"
    log_info "配置hdfs-site.xml: $hdfs_site_file"
    
    cat > "$hdfs_site_file" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
    <property>
        <name>dfs.replication</name>
        <value>1</value>
    </property>
    <property>
        <name>dfs.namenode.name.dir</name>
        <value>/opt/hadoop/data/dfs/name</value>
    </property>
    <property>
        <name>dfs.datanode.data.dir</name>
        <value>/opt/hadoop/data/dfs/data</value>
    </property>
    <property>
        <name>dfs.namenode.rpc-address</name>
        <value>${DOCKER_CONTAINER}:9000</value>
    </property>
    <property>
        <name>dfs.namenode.servicerpc-address</name>
        <value>${DOCKER_CONTAINER}:9001</value>
    </property>
    <property>
        <name>dfs.namenode.http-address</name>
        <value>${DOCKER_CONTAINER}:9870</value>
    </property>
    <property>
        <name>dfs.namenode.secondary.http-address</name>
        <value>${DOCKER_CONTAINER}:9868</value>
    </property>
</configuration>
EOF
    
    if [ $? -ne 0 ]; then
        log_error "配置hdfs-site.xml失败"
        return 1
    fi
    
    log_success "Hadoop XML配置文件配置完成"
    return 0
}

# Docker容器配置
DOCKER_CONTAINER="hadoop-single"

# 配置变量
HADOOP_HOME="/opt/hadoop"
HBASE_HOME="/opt/hbase"
SPARK_HOME="/opt/spark"
HDFS_HBASE_DIR="/hbase"
HDFS_SPARK_DIR="/sparklog"
LOG_DIR="$(pwd)"
LOG_FILE="${LOG_DIR}/env-init-$(date '+%Y%m%d_%H%M%S').log"

# 配置文件路径
HADOOP_CONF_DIR="$HADOOP_HOME/etc/hadoop"
SPARK_CONF_DIR="$SPARK_HOME/conf"
HBASE_CONF_DIR="$HBASE_HOME/conf"

# 初始化日志文件
init_log() {
    echo "=====================================" > "$LOG_FILE"
    echo "大数据组件单机模式初始化日志" >> "$LOG_FILE"
    echo "开始时间: $(date '+%Y-%m-%d %H:%M:%S')" >> "$LOG_FILE"
    echo "日志文件: $LOG_FILE" >> "$LOG_FILE"
    echo "Docker容器: $DOCKER_CONTAINER" >> "$LOG_FILE"
    echo "=====================================" >> "$LOG_FILE"
    echo "" >> "$LOG_FILE"
}

# 日志函数
log_info() {
    local msg="[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1"
    echo -e "${BLUE}${msg}${NC}"
    echo "$msg" >> "$LOG_FILE"
}

log_success() {
    local msg="[SUCCESS] $(date '+%Y-%m-%d %H:%M:%S') - $1"
    echo -e "${GREEN}${msg}${NC}"
    echo "$msg" >> "$LOG_FILE"
}

log_warning() {
    local msg="[WARNING] $(date '+%Y-%m-%d %H:%M:%S') - $1"
    echo -e "${YELLOW}${msg}${NC}"
    echo "$msg" >> "$LOG_FILE"
}

log_error() {
    local msg="[ERROR] $(date '+%Y-%m-%d %H:%M:%S') - $1"
    echo -e "${RED}${msg}${NC}"
    echo "$msg" >> "$LOG_FILE"
}

log_step() {
    local step_msg="步骤 $1: $2"
    echo -e "\n${PURPLE}========================================${NC}"
    echo -e "${PURPLE}${step_msg}${NC}"
    echo -e "${PURPLE}========================================${NC}"
    
    echo "" >> "$LOG_FILE"
    echo "========================================" >> "$LOG_FILE"
    echo "$step_msg" >> "$LOG_FILE"
    echo "========================================" >> "$LOG_FILE"
}

# Docker命令执行函数
docker_exec() {
    local cmd="$1"
    docker exec "$DOCKER_CONTAINER" bash -c "$cmd"
    return $?
}

# 带环境变量的Docker命令执行函数
docker_exec_with_env() {
    local cmd="$1"
    # 确保设置JAVA_HOME和HADOOP用户环境变量
    local env_cmd="export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 && \
                  export HDFS_NAMENODE_USER=root && \
                  export HDFS_DATANODE_USER=root && \
                  export HDFS_SECONDARYNAMENODE_USER=root && \
                  export YARN_RESOURCEMANAGER_USER=root && \
                  export YARN_NODEMANAGER_USER=root && \
                  $cmd"
    docker exec "$DOCKER_CONTAINER" bash -c "$env_cmd"
    return $?
}

# 执行命令并记录输出到日志
exec_and_log() {
    local cmd="$1"
    local description="$2"
    
    if [ -n "$description" ]; then
        log_info "执行: $description"
        echo "[COMMAND] $description" >> "$LOG_FILE"
    fi
    
    echo "[CMD] $cmd" >> "$LOG_FILE"
    
    # 执行命令并捕获输出
    local output
    output=$(eval "$cmd" 2>&1)
    local exit_code=$?
    
    # 记录输出到日志文件
    if [ -n "$output" ]; then
        echo "[OUTPUT] $output" >> "$LOG_FILE"
    fi
    
    echo "[EXIT_CODE] $exit_code" >> "$LOG_FILE"
    echo "" >> "$LOG_FILE"
    
    return $exit_code
}

# 检查命令是否存在
check_command() {
    if ! command -v "$1" &> /dev/null; then
        log_error "命令 '$1' 未安装或不在PATH中"
        return 1
    fi
    return 0
}

# 检查Docker容器状态
check_docker_container() {
    log_info "检查Docker容器 '$DOCKER_CONTAINER' 状态..."
    
    # 检查Docker命令是否可用
    if ! check_command "docker"; then
        log_error "Docker命令不可用，请确保Docker已安装"
        return 1
    fi
    
    # 检查容器是否存在
    if ! docker ps -a --format '{{.Names}}' | grep -q "^${DOCKER_CONTAINER}$"; then
        log_error "Docker容器 '$DOCKER_CONTAINER' 不存在"
        return 1
    fi
    
    # 检查容器是否运行
    if ! docker ps --format '{{.Names}}' | grep -q "^${DOCKER_CONTAINER}$"; then
        log_warning "Docker容器 '$DOCKER_CONTAINER' 未运行，尝试启动..."
        if ! docker start "$DOCKER_CONTAINER"; then
            log_error "无法启动Docker容器 '$DOCKER_CONTAINER'"
            return 1
        fi
        log_success "Docker容器 '$DOCKER_CONTAINER' 已启动"
    else
        log_success "Docker容器 '$DOCKER_CONTAINER' 正在运行"
    fi
    
    return 0
}

# 检查容器中的环境变量
check_container_env() {
    log_info "检查容器中的环境变量..."
    
    # 检查JAVA_HOME是否设置
    if ! docker_exec "echo \$JAVA_HOME | grep -q '.'"; then
        log_warning "容器中的JAVA_HOME环境变量未设置或为空"
        # 尝试自动检测Java路径
        if docker_exec "[ -d /usr/lib/jvm/java-8-openjdk-amd64 ]"; then
            log_info "检测到Java路径: /usr/lib/jvm/java-8-openjdk-amd64"
        else
            log_error "无法在容器中找到有效的Java安装路径"
            return 1
        fi
    else
        local java_home=$(docker_exec "echo \$JAVA_HOME")
        log_info "容器中的JAVA_HOME环境变量: $java_home"
    fi
    
    return 0
}

# 配置Hadoop环境
configure_hadoop_env() {
    log_info "配置Hadoop环境变量..."
    
    # hadoop-env.sh配置
    local hadoop_env_file="${HADOOP_ENV_DIR}/hadoop-env.sh"
    log_info "配置Hadoop环境变量文件: $hadoop_env_file"
    
    # 检查文件是否存在
    if [ ! -f "$hadoop_env_file" ]; then
        log_error "Hadoop环境变量文件不存在: $hadoop_env_file"
        return 1
    fi
    
    # 配置hadoop-env.sh
    cat > "$hadoop_env_file" << EOF
# Java路径配置
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

# Hadoop用户配置
export HDFS_NAMENODE_USER=root
export HDFS_DATANODE_USER=root
export HDFS_SECONDARYNAMENODE_USER=root
export YARN_RESOURCEMANAGER_USER=root
export YARN_NODEMANAGER_USER=root

# 内存配置
export HADOOP_HEAPSIZE=1024m
export HADOOP_NAMENODE_INIT_HEAPSIZE=1024m
EOF
    
    if [ $? -ne 0 ]; then
        log_error "配置Hadoop环境变量文件失败"
        return 1
    fi
    
    # 配置Hadoop的XML文件
    configure_hadoop_xml
    if [ $? -ne 0 ]; then
        log_error "配置Hadoop XML文件失败"
        return 1
    fi
    
    log_success "Hadoop环境变量配置完成"
    return 0
}

# 配置Spark环境
configure_spark_env() {
    log_info "配置Spark环境..."
    
    # 确保Spark配置目录存在
    if [ ! -d "${SPARK_ENV_DIR}" ]; then
        log_warning "Spark配置目录不存在: ${SPARK_ENV_DIR}"
        if ! mkdir -p "${SPARK_ENV_DIR}"; then
            log_error "创建Spark配置目录失败"
            return 1
        fi
    fi
    
    # 配置spark-env.sh
    local spark_env_file="${SPARK_ENV_DIR}/spark-env.sh"
    log_info "配置Spark环境变量文件: $spark_env_file"
    
    cat > "$spark_env_file" << EOF
#!/usr/bin/env bash

# Java和Hadoop环境
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export HADOOP_HOME=/opt/hadoop
export HADOOP_CONF_DIR=/opt/hadoop/etc/hadoop
export YARN_CONF_DIR=/opt/hadoop/etc/hadoop

# Spark Worker配置
export SPARK_WORKER_CORES=2
export SPARK_WORKER_MEMORY=1g
export SPARK_WORKER_PORT=7078
export SPARK_WORKER_WEBUI_PORT=8081
export SPARK_WORKER_DIR=/opt/spark/work

# 历史服务器配置
export SPARK_HISTORY_OPTS="-Dspark.history.fs.logDirectory=hdfs://hadoop-single:9000/sparklog/ \\
                           -Dspark.history.fs.cleaner.enabled=true \\
                           -Dspark.history.fs.cleaner.interval=1d \\
                           -Dspark.history.fs.cleaner.maxAge=7d"

# 内存配置
export SPARK_DRIVER_MEMORY=1g
export SPARK_EXECUTOR_MEMORY=1g

# 日志配置
export SPARK_LOG_DIR=/opt/spark/logs
EOF
    
    if [ $? -ne 0 ]; then
        log_error "配置Spark环境变量文件失败"
        return 1
    fi
    
    # 配置spark-defaults.conf
    local spark_defaults_file="${SPARK_ENV_DIR}/spark-defaults.conf"
    log_info "配置Spark默认配置文件: $spark_defaults_file"
    
    cat > "$spark_defaults_file" << EOF
# 基础配置
spark.master                    spark://hadoop-single:7077
spark.eventLog.enabled          true
spark.eventLog.dir              hdfs://hadoop-single:9000/sparklog/
spark.eventLog.compress         true

# 序列化配置
spark.serializer                org.apache.spark.serializer.KryoSerializer

# SQL适应性优化
spark.sql.adaptive.enabled                     true
spark.sql.adaptive.coalescePartitions.enabled  true
spark.sql.adaptive.skewJoin.enabled            true

# 动态资源分配
spark.dynamicAllocation.enabled         true
spark.dynamicAllocation.minExecutors    1
spark.dynamicAllocation.maxExecutors    5
spark.dynamicAllocation.initialExecutors 1

# 历史服务器配置
spark.history.provider              org.apache.spark.deploy.history.FsHistoryProvider
spark.history.fs.logDirectory       hdfs://hadoop-single:9000/sparklog/
spark.history.fs.update.interval    10s

# 性能调优
spark.driver.memory                 1g
spark.driver.cores                  1
spark.executor.memory               1g
spark.executor.cores                1
spark.executor.instances            1

# 网络配置
spark.network.timeout           300s
spark.rpc.askTimeout            300s
EOF
    
    if [ $? -ne 0 ]; then
        log_error "配置Spark默认配置文件失败"
        return 1
    fi
    
    # 设置执行权限
    chmod +x "$spark_env_file"
    
    log_success "Spark环境配置完成"
    return 0
}

# 配置HBase环境
configure_hbase_env() {
    log_info "配置HBase环境..."
    
    # 确保HBase配置目录存在
    if [ ! -d "${HBASE_ENV_DIR}" ]; then
        log_warning "HBase配置目录不存在: ${HBASE_ENV_DIR}"
        if ! mkdir -p "${HBASE_ENV_DIR}"; then
            log_error "创建HBase配置目录失败"
            return 1
        fi
    fi
    
    # 配置hbase-env.sh
    local hbase_env_file="${HBASE_ENV_DIR}/hbase-env.sh"
    log_info "配置HBase环境变量文件: $hbase_env_file"
    
    cat > "$hbase_env_file" << EOF
#!/usr/bin/env bash

# Java和Hadoop环境
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export HBASE_MANAGES_ZK=true
export HBASE_HEAPSIZE=1024m

# 日志配置
export HBASE_LOG_DIR=/opt/hbase/logs
EOF
    
    if [ $? -ne 0 ]; then
        log_error "配置HBase环境变量文件失败"
        return 1
    fi
    
    # 配置hbase-site.xml
    local hbase_site_file="${HBASE_ENV_DIR}/hbase-site.xml"
    log_info "配置HBase站点文件: $hbase_site_file"
    
    cat > "$hbase_site_file" << EOF
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
  <property>
    <name>hbase.rootdir</name>
    <value>hdfs://hadoop-single:9000/hbase</value>
  </property>
  <property>
    <name>hbase.cluster.distributed</name>
    <value>false</value>
  </property>
  <property>
    <name>hbase.tmp.dir</name>
    <value>/opt/hbase/tmp</value>
  </property>
  <property>
    <name>hbase.unsafe.stream.capability.enforce</name>
    <value>false</value>
  </property>
</configuration>
EOF
    
    if [ $? -ne 0 ]; then
        log_error "配置HBase站点文件失败"
        return 1
    fi
    
    # 设置执行权限
    chmod +x "$hbase_env_file"
    
    log_success "HBase环境配置完成"
    return 0
}

# 同步Hadoop配置到HBase目录
sync_hadoop_hbase_configs() {
    log_info "同步Hadoop配置文件到HBase配置目录..."
    
    # 检查Hadoop配置文件是否存在
    if [ ! -f "$HADOOP_ENV_DIR/core-site.xml" ]; then
        log_error "Hadoop配置文件不存在: $HADOOP_ENV_DIR/core-site.xml"
        return 1
    fi
    
    if [ ! -f "$HADOOP_ENV_DIR/hdfs-site.xml" ]; then
        log_error "Hadoop配置文件不存在: $HADOOP_ENV_DIR/hdfs-site.xml"
        return 1
    fi
    
    # 确保HBase配置目录存在
    if [ ! -d "$HBASE_ENV_DIR" ]; then
        log_warning "HBase配置目录不存在，正在创建: $HBASE_ENV_DIR"
        mkdir -p "$HBASE_ENV_DIR"
    fi
    
    # 复制配置文件
    cp "$HADOOP_ENV_DIR/core-site.xml" "$HBASE_ENV_DIR/"
    if [ $? -ne 0 ]; then
        log_error "复制core-site.xml到HBase配置目录失败"
        return 1
    fi
    
    cp "$HADOOP_ENV_DIR/hdfs-site.xml" "$HBASE_ENV_DIR/"
    if [ $? -ne 0 ]; then
        log_error "复制hdfs-site.xml到HBase配置目录失败"
        return 1
    fi
    
    log_success "Hadoop配置文件已成功同步到HBase配置目录"
    return 0
}

# ====================== Hadoop 初始化函数 ======================
init_hadoop() {
    log_step "1" "初始化Hadoop单机模式"
    
    # 配置Hadoop环境
    configure_hadoop_env
    if [ $? -ne 0 ]; then
        log_error "Hadoop环境配置失败"
        return 1
    fi
    
    # 检查Hadoop是否已安装在容器中
    log_info "检查容器中的Hadoop安装..."
    if ! docker_exec "[ -d \"$HADOOP_HOME\" ]"; then
        log_error "容器中的Hadoop未安装或目录不存在: $HADOOP_HOME"
        return 1
    fi
    
    log_info "停止可能正在运行的Hadoop服务..."
    exec_and_log "docker exec $DOCKER_CONTAINER bash -c 'export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 && $HADOOP_HOME/sbin/stop-all.sh'" "停止所有Hadoop服务"
    
    log_info "配置Hadoop单机模式..."
    
    # 格式化NameNode (如果需要)
    log_info "格式化NameNode..."
    if ! docker_exec "[ -d \"$HADOOP_HOME/data/dfs/name\" ] && [ \"\$(ls -A $HADOOP_HOME/data/dfs/name 2>/dev/null)\" != \"\" ]"; then
        exec_and_log "docker exec $DOCKER_CONTAINER bash -c 'export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 && $HADOOP_HOME/bin/hdfs namenode -format -force'" "格式化NameNode"
        if [ $? -ne 0 ]; then
            log_error "NameNode格式化失败"
            return 1
        fi
        log_success "NameNode格式化成功"
    else
        log_warning "NameNode目录已存在，跳过格式化"
    fi
    
    # 启动HDFS
    log_info "启动Hadoop单机模式HDFS..."
    exec_and_log "docker_exec_with_env \"$HADOOP_HOME/sbin/start-dfs.sh\"" "启动HDFS"
    if [ $? -ne 0 ]; then
        log_error "HDFS启动失败"
        return 1
    fi
    
    # 启动YARN
    log_info "启动YARN..."
    exec_and_log "docker_exec_with_env \"$HADOOP_HOME/sbin/start-yarn.sh\"" "启动YARN"
    if [ $? -ne 0 ]; then
        log_warning "YARN启动失败，但继续执行后续步骤"
    fi
    
    # 验证Hadoop服务状态
    log_info "验证Hadoop服务状态..."
    exec_and_log "docker_exec_with_env \"$HADOOP_HOME/bin/hdfs dfsadmin -report\"" "检查HDFS状态"
    exec_and_log "docker_exec_with_env \"$HADOOP_HOME/bin/yarn node -list\"" "检查YARN节点状态"
    
    log_success "Hadoop单机模式初始化完成"
    return 0
}

# ====================== HBase 初始化函数 ======================
init_hbase() {
    log_step "2" "初始化HBase单机环境"
    
    # 配置HBase环境
    configure_hbase_env
    if [ $? -ne 0 ]; then
        log_error "HBase环境配置失败"
        return 1
    fi
    
    # 检查HBase目录是否存在
    log_info "检查容器中的HBase安装..."
    if ! docker_exec "[ -d \"$HBASE_HOME\" ]"; then
        log_warning "容器中的HBase安装目录不存在: $HBASE_HOME"
        log_info "跳过HBase初始化"
        return 0
    fi
    
    # 检查HDFS是否正常运行
    log_info "检查HDFS服务状态..."
    if ! docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -ls / &>/dev/null"; then
        log_error "HDFS服务未正常运行，请确保Hadoop已正确启动"
        return 1
    fi
    
    # 创建HBase在HDFS上的目录
    log_info "创建HBase HDFS目录: $HDFS_HBASE_DIR"
    if docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -test -d \"$HDFS_HBASE_DIR\"" 2>/dev/null; then
        log_warning "目录 '$HDFS_HBASE_DIR' 已存在"
    else
        if docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -mkdir -p \"$HDFS_HBASE_DIR\""; then
            log_success "HBase目录创建成功: $HDFS_HBASE_DIR"
        else
            log_error "HBase目录创建失败: $HDFS_HBASE_DIR"
            return 1
        fi
    fi
    
    # 设置目录权限
    log_info "设置HBase目录权限: $HDFS_HBASE_DIR (755)"
    if docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -chmod 755 \"$HDFS_HBASE_DIR\""; then
        log_success "HBase目录权限设置成功"
    else
        log_error "HBase目录权限设置失败"
        return 1
    fi
    
    # 验证目录
    log_info "验证HBase目录设置..."
    docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -ls -d \"$HDFS_HBASE_DIR\""
    
    log_success "HBase单机环境初始化完成"
    return 0
}

# ====================== Spark 初始化函数 ======================
init_spark() {
    log_step "3" "初始化Spark单机环境"
    
    # 配置Spark环境
    configure_spark_env
    if [ $? -ne 0 ]; then
        log_error "Spark环境配置失败"
        return 1
    fi
    
    # 检查Spark目录是否存在
    log_info "检查容器中的Spark安装..."
    if ! docker_exec "[ -d \"$SPARK_HOME\" ]"; then
        log_warning "容器中的Spark安装目录不存在: $SPARK_HOME"
        log_info "跳过Spark初始化"
        return 0
    fi
    
    # 检查HDFS是否正常运行
    log_info "检查HDFS服务状态..."
    if ! docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -ls / &>/dev/null"; then
        log_error "HDFS服务未正常运行，请确保Hadoop已正确启动"
        return 1
    fi
    
    # 创建Spark在HDFS上的日志目录
    log_info "创建Spark日志目录: $HDFS_SPARK_DIR"
    if docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -test -d \"$HDFS_SPARK_DIR\"" 2>/dev/null; then
        log_warning "目录 '$HDFS_SPARK_DIR' 已存在"
    else
        if docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -mkdir -p \"$HDFS_SPARK_DIR\""; then
            log_success "Spark日志目录创建成功: $HDFS_SPARK_DIR"
        else
            log_error "Spark日志目录创建失败: $HDFS_SPARK_DIR"
            return 1
        fi
    fi
    
    # 设置目录权限 - 更安全的权限
    log_info "设置Spark日志目录权限: $HDFS_SPARK_DIR (755)"
    if docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -chmod 755 \"$HDFS_SPARK_DIR\""; then
        log_success "Spark日志目录权限设置成功"
    else
        log_error "Spark日志目录权限设置失败"
        return 1
    fi
    
    # 验证目录
    log_info "验证Spark日志目录设置..."
    docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -ls -d \"$HDFS_SPARK_DIR\""
    
    # 显示Spark配置建议
    echo -e "${CYAN}Spark配置建议:${NC}"
    echo "在Spark配置中设置以下参数："
    echo "  spark.eventLog.enabled=true"
    echo "  spark.eventLog.dir=hdfs://$DOCKER_CONTAINER:9000$HDFS_SPARK_DIR"
    echo "  spark.history.fs.logDirectory=hdfs://$DOCKER_CONTAINER:9000$HDFS_SPARK_DIR"
    
    log_success "Spark单机环境初始化完成"
    return 0
}

# ====================== 主函数 ======================
main() {
    echo -e "${CYAN}"
    echo "  ____            _             _       _ _   "
    echo " | __ )  ___  ___| |_ ___ _   _| |__   (_) |_ "
    echo " |  _ \ / _ \/ __| __/ __| | | | '_ \  | | __|"
    echo " | |_) | (_) \__ \ |_\__ \ |_| | |_) | | | |_ "
    echo " |____/ \___/|___/\__|___/\__,_|_.__/  |_|\__|"
    echo -e "${NC}"
    echo -e "${CYAN}大数据环境单机模式初始化脚本 (Docker版)${NC}\n"
    
    # 初始化日志
    init_log
    log_info "大数据环境单机模式初始化脚本启动..."
    
    # 配置Hadoop XML文件
    configure_hadoop_xml
    if [ $? -ne 0 ]; then
        log_error "Hadoop XML配置文件配置失败"
        exit 1
    fi
    
    # 同步Hadoop和HBase配置文件
    sync_hadoop_hbase_configs
    if [ $? -ne 0 ]; then
        log_warning "Hadoop和HBase配置文件同步失败，但继续执行后续步骤"
    fi
    
    # 检查必要命令
    log_info "检查必要命令..."
    if ! check_command "bash"; then
        log_error "bash命令不可用，脚本无法继续"
        exit 1
    fi
    
    # 检查Docker容器
    check_docker_container
    docker_status=$?
    
    if [ $docker_status -ne 0 ]; then
        log_error "Docker容器检查失败，脚本无法继续"
        exit 1
    fi
    
    # 检查容器环境
    check_container_env
    env_status=$?
    
    if [ $env_status -ne 0 ]; then
        log_error "容器环境检查失败，脚本无法继续"
        exit 1
    fi
    
    # 依次初始化各组件
    init_hadoop
    hadoop_status=$?
    
    # 只有当Hadoop初始化成功后才继续
    if [ $hadoop_status -eq 0 ]; then
        init_hbase
        init_spark
    else
        log_error "Hadoop初始化失败，跳过后续组件初始化"
    fi
    
    # 显示HDFS目录概览
    log_info "HDFS根目录内容:"
    docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -ls /"
    
    # 获取容器IP
    CONTAINER_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $DOCKER_CONTAINER)
    
    # 总结
    echo -e "\n${GREEN}========================================${NC}"
    echo -e "${GREEN}  大数据环境单机模式初始化完成！${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo -e "${YELLOW}访问信息：${NC}"
    echo -e "  • 容器名: $DOCKER_CONTAINER"
    echo -e "  • 容器IP: $CONTAINER_IP"
    echo -e "  • NameNode Web UI: http://$CONTAINER_IP:9870"
    echo -e "  • ResourceManager Web UI: http://$CONTAINER_IP:8088"
    echo -e "  • DataNode Web UI: http://$CONTAINER_IP:9864"
    
    log_success "大数据环境单机模式初始化脚本执行完成！"
    log_info "完成时间: $(date '+%Y-%m-%d %H:%M:%S')"
    log_info "日志已保存到: $LOG_FILE"
    echo "" >> "$LOG_FILE"
    echo "=====================================" >> "$LOG_FILE"
    echo "脚本执行完成" >> "$LOG_FILE"
    echo "结束时间: $(date '+%Y-%m-%d %H:%M:%S')" >> "$LOG_FILE"
    echo "=====================================" >> "$LOG_FILE"
}

# 错误处理
set -e
trap 'log_error "脚本执行过程中发生错误，位置: $BASH_COMMAND"' ERR

# 允许脚本在某些命令失败时继续运行
set +e

# 运行主函数
main "$@"
