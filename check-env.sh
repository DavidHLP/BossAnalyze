#!/bin/bash

#######################################################################
# 大数据组件单机环境检查脚本
# 
# 功能说明：
# 1. 检查Docker容器状态
# 2. 检查Hadoop服务状态（HDFS、YARN）
# 3. 检查HBase服务状态
# 4. 检查Spark服务状态
# 5. 检查HDFS目录和权限
#
# 使用方法：bash check-env.sh
# 作者：DavidHLP
# 版本：1.0
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

# 环境变量
DOCKER_CONTAINER="hadoop-single"
HADOOP_HOME="/opt/hadoop"
HBASE_HOME="/opt/hbase"
SPARK_HOME="/opt/spark"
LOG_DIR="$(pwd)"
LOG_FILE="${LOG_DIR}/check-env-$(date '+%Y%m%d_%H%M%S').log"

# 初始化日志文件
init_log() {
    echo "=====================================" > "$LOG_FILE"
    echo "大数据组件环境检查日志" >> "$LOG_FILE"
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
    log_step "1" "检查Docker容器状态"
    
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
        log_warning "Docker容器 '$DOCKER_CONTAINER' 未运行"
        return 1
    fi
    
    log_success "Docker容器 '$DOCKER_CONTAINER' 正在运行"
    
    # 显示容器信息
    local container_info=$(docker inspect $DOCKER_CONTAINER --format "ID: {{.Id}}\nIP: {{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}\n运行时间: {{.State.StartedAt}}")
    echo -e "${CYAN}容器信息:${NC}"
    echo -e "$container_info"
    
    return 0
}

# 检查Hadoop服务状态
check_hadoop_services() {
    log_step "2" "检查Hadoop服务状态"
    
    # 检查HDFS服务
    log_info "检查HDFS服务状态..."
    if ! docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfsadmin -safemode get" &>/dev/null; then
        log_error "无法连接到HDFS服务，请确保服务已启动"
        return 1
    fi
    
    # 检查NameNode状态
    log_info "检查NameNode状态..."
    local nn_status=$(docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfsadmin -report" | grep "Live datanodes")
    if [ -z "$nn_status" ]; then
        log_error "无法获取NameNode状态信息"
    else
        log_success "NameNode状态: $nn_status"
    fi
    
    # 检查DataNode状态
    log_info "检查DataNode状态..."
    local dn_count=$(docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfsadmin -report" | grep "Live datanodes" | awk '{print $3}')
    if [ -z "$dn_count" ]; then
        log_error "无法获取DataNode数量"
    else
        log_success "活跃DataNode数量: $dn_count"
    fi
    
    # 检查HDFS容量
    log_info "检查HDFS容量..."
    local hdfs_capacity=$(docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -df -h /" | grep -v "Filesystem" | awk '{print "总容量: "$2", 已用: "$3" ("$4"), 可用: "$5}')
    if [ -z "$hdfs_capacity" ]; then
        log_warning "无法获取HDFS容量信息"
    else
        log_success "HDFS容量: $hdfs_capacity"
    fi
    
    # 检查YARN服务
    log_info "检查YARN服务状态..."
    if ! docker_exec_with_env "$HADOOP_HOME/bin/yarn node -list" &>/dev/null; then
        log_warning "无法连接到YARN服务，可能未启动或配置不正确"
    else
        local yarn_nodes=$(docker_exec_with_env "$HADOOP_HOME/bin/yarn node -list" | grep "Total Nodes" | awk '{print $3}')
        log_success "YARN节点总数: $yarn_nodes"
    fi
    
    return 0
}

# 检查HBase服务状态
check_hbase_services() {
    log_step "3" "检查HBase服务状态"
    
    # 检查HBase是否安装
    if ! docker_exec "[ -d \"$HBASE_HOME\" ]"; then
        log_warning "HBase目录不存在: $HBASE_HOME，跳过HBase检查"
        return 0
    fi
    
    # 检查HBase服务状态
    log_info "检查HBase服务状态..."
    if ! docker_exec "$HBASE_HOME/bin/hbase version" &>/dev/null; then
        log_error "无法执行HBase命令，可能未正确安装"
        return 1
    fi
    
    # 检查HBase是否运行
    if ! docker_exec "ps -ef | grep HMaster | grep -v grep" &>/dev/null; then
        log_warning "HBase Master进程未运行"
    else
        log_success "HBase Master进程正在运行"
    fi
    
    # 检查HBase表
    log_info "检查HBase表..."
    if docker_exec "$HBASE_HOME/bin/hbase shell -n list" &>/dev/null; then
        local tables=$(docker_exec "$HBASE_HOME/bin/hbase shell -n 'list'" | grep -v "TABLE" | grep -v "^$" | grep -v "row(s)" | grep -v "hbase")
        if [ -z "$tables" ]; then
            log_info "HBase中没有用户创建的表"
        else
            log_success "HBase中的表: $tables"
        fi
    else
        log_warning "无法获取HBase表列表，HBase服务可能未启动"
    fi
    
    return 0
}

# 检查Spark服务状态
check_spark_services() {
    log_step "4" "检查Spark服务状态"
    
    # 检查Spark是否安装
    if ! docker_exec "[ -d \"$SPARK_HOME\" ]"; then
        log_warning "Spark目录不存在: $SPARK_HOME，跳过Spark检查"
        return 0
    fi
    
    # 检查Spark版本
    log_info "检查Spark版本..."
    if ! docker_exec "$SPARK_HOME/bin/spark-submit --version" &>/dev/null; then
        log_error "无法执行Spark命令，可能未正确安装"
        return 1
    else
        local spark_version=$(docker_exec "$SPARK_HOME/bin/spark-submit --version 2>&1" | grep "version" | head -1)
        log_success "Spark版本: $spark_version"
    fi
    
    # 检查Spark Master是否运行
    log_info "检查Spark Master进程..."
    if ! docker_exec "ps -ef | grep spark.deploy.master.Master | grep -v grep" &>/dev/null; then
        log_warning "Spark Master进程未运行"
    else
        log_success "Spark Master进程正在运行"
    fi
    
    # 检查Spark Worker是否运行
    log_info "检查Spark Worker进程..."
    if ! docker_exec "ps -ef | grep spark.deploy.worker.Worker | grep -v grep" &>/dev/null; then
        log_warning "Spark Worker进程未运行"
    else
        local worker_count=$(docker_exec "ps -ef | grep spark.deploy.worker.Worker | grep -v grep | wc -l")
        log_success "Spark Worker进程数: $worker_count"
    fi
    
    # 检查Spark历史服务器
    log_info "检查Spark历史服务器..."
    if ! docker_exec "ps -ef | grep spark.history.server.HistoryServer | grep -v grep" &>/dev/null; then
        log_warning "Spark历史服务器未运行"
    else
        log_success "Spark历史服务器正在运行"
    fi
    
    return 0
}

# 检查HDFS目录和权限
check_hdfs_directories() {
    log_step "5" "检查HDFS目录和权限"
    
    # 检查HDFS服务是否可用
    if ! docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -ls /" &>/dev/null; then
        log_error "无法访问HDFS，请确保服务已启动"
        return 1
    fi
    
    # 列出HDFS根目录
    log_info "列出HDFS根目录内容..."
    local root_dirs=$(docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -ls /" | grep -v "Found")
    echo -e "${CYAN}HDFS根目录内容:${NC}"
    echo -e "$root_dirs"
    
    # 检查HBase目录
    log_info "检查HBase目录..."
    if docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -test -d /hbase" &>/dev/null; then
        local hbase_dir_info=$(docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -ls -d /hbase")
        log_success "HBase目录存在: $hbase_dir_info"
    else
        log_warning "HBase目录不存在，可能需要初始化"
    fi
    
    # 检查Spark日志目录
    log_info "检查Spark日志目录..."
    if docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -test -d /sparklog" &>/dev/null; then
        local spark_dir_info=$(docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -ls -d /sparklog")
        log_success "Spark日志目录存在: $spark_dir_info"
    else
        log_warning "Spark日志目录不存在，可能需要初始化"
    fi
    
    # 检查HDFS健康状态
    log_info "检查HDFS健康状态..."
    local hdfs_health=$(docker_exec_with_env "$HADOOP_HOME/bin/hdfs fsck / | grep 'Status'")
    if [ -n "$hdfs_health" ]; then
        log_success "HDFS健康状态: $hdfs_health"
    else
        log_warning "无法获取HDFS健康状态"
    fi
    
    return 0
}

# ====================== 主函数 ======================
main() {
    echo -e "${CYAN}"
    echo "  ____            _        ____ _               _    "
    echo " | __ )  ___  ___| |_ ___ / ___| |__   ___  ___| | __"
    echo " |  _ \ / _ \/ __| __/ __| |   | '_ \ / _ \/ __| |/ /"
    echo " | |_) | (_) \__ \ |_\__ \ |___| | | |  __/ (__|   < "
    echo " |____/ \___/|___/\__|___/\____|_| |_|\___|\___|_|\_\\"
    echo -e "${NC}"
    echo -e "${CYAN}大数据环境检查脚本${NC}\n"
    
    # 初始化日志
    init_log
    log_info "大数据环境检查脚本启动..."
    
    # 检查环境
    check_docker_container
    docker_status=$?
    
    # 只有当Docker容器正常运行时才继续检查
    if [ $docker_status -eq 0 ]; then
        check_hadoop_services
        check_hbase_services
        check_spark_services
        check_hdfs_directories
    else
        log_error "Docker容器检查失败，无法继续检查服务状态"
    fi
    
    # 获取容器IP
    CONTAINER_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $DOCKER_CONTAINER)
    
    # 总结
    echo -e "\n${GREEN}========================================${NC}"
    echo -e "${GREEN}  大数据环境检查完成！${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo -e "${YELLOW}访问信息：${NC}"
    echo -e "  • 容器名: $DOCKER_CONTAINER"
    echo -e "  • 容器IP: $CONTAINER_IP"
    echo -e "  • NameNode Web UI: http://$CONTAINER_IP:9870"
    echo -e "  • ResourceManager Web UI: http://$CONTAINER_IP:8088"
    echo -e "  • DataNode Web UI: http://$CONTAINER_IP:9864"
    echo -e "  • Spark Master Web UI: http://$CONTAINER_IP:8080"
    echo -e "  • Spark History Server: http://$CONTAINER_IP:18080"
    
    log_success "大数据环境检查脚本执行完成！"
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
