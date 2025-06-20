#!/bin/bash

#######################################################################
# 大数据组件启动脚本
# 
# 功能说明：
# 1. 启动Hadoop服务（HDFS和YARN）
# 2. 启动HBase服务
# 3. 启动Spark服务
# 4. 启动Spark历史服务器
#
# 使用方法：bash start-env.sh [选项]
# 选项：
#   --hadoop    仅启动Hadoop
#   --hbase     仅启动HBase
#   --spark     仅启动Spark
#   --all       启动所有服务（默认）
#
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
LOG_FILE="${LOG_DIR}/start-env-$(date '+%Y%m%d_%H%M%S').log"

# 初始化日志文件
init_log() {
    echo "=====================================" > "$LOG_FILE"
    echo "大数据组件启动日志" >> "$LOG_FILE"
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

# 启动Hadoop服务
start_hadoop() {
    log_step "1" "启动Hadoop服务"
    
    # 检查Hadoop是否已安装
    log_info "检查Hadoop安装..."
    if ! docker_exec "[ -d \"$HADOOP_HOME\" ]"; then
        log_error "Hadoop目录不存在: $HADOOP_HOME"
        return 1
    fi
    
    # 停止可能正在运行的Hadoop服务
    log_info "停止可能正在运行的Hadoop服务..."
    docker_exec_with_env "$HADOOP_HOME/sbin/stop-all.sh" &>/dev/null
    
    # 启动HDFS
    log_info "启动HDFS服务..."
    if ! docker_exec_with_env "$HADOOP_HOME/sbin/start-dfs.sh"; then
        log_error "HDFS服务启动失败"
        return 1
    fi
    
    # 等待HDFS服务完全启动
    log_info "等待HDFS服务完全启动..."
    sleep 5
    
    # 验证HDFS是否正在运行
    if ! docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfsadmin -report" &>/dev/null; then
        log_error "HDFS服务启动后验证失败"
        return 1
    fi
    
    log_success "HDFS服务已成功启动"
    
    # 启动YARN
    log_info "启动YARN服务..."
    if ! docker_exec_with_env "$HADOOP_HOME/sbin/start-yarn.sh"; then
        log_warning "YARN服务启动可能失败，但继续执行"
    else
        log_success "YARN服务已成功启动"
    fi
    
    # 验证YARN是否正在运行
    if ! docker_exec_with_env "$HADOOP_HOME/bin/yarn node -list" &>/dev/null; then
        log_warning "YARN服务启动后验证失败，但继续执行"
    else
        local yarn_status=$(docker_exec_with_env "$HADOOP_HOME/bin/yarn node -list" | grep "Total Nodes")
        log_success "YARN状态: $yarn_status"
    fi
    
    # 显示HDFS状态摘要
    log_info "HDFS状态摘要:"
    docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfsadmin -report | head -n 15"
    
    return 0
}

# 启动HBase服务
start_hbase() {
    log_step "2" "启动HBase服务"
    
    # 检查HBase是否已安装
    log_info "检查HBase安装..."
    if ! docker_exec "[ -d \"$HBASE_HOME\" ]"; then
        log_warning "HBase目录不存在: $HBASE_HOME，跳过启动HBase"
        return 0
    fi
    
    # 检查HDFS是否正在运行
    log_info "检查HDFS服务状态..."
    if ! docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -ls /" &>/dev/null; then
        log_error "HDFS服务未运行，HBase依赖于HDFS，请先启动Hadoop"
        return 1
    fi
    
    # 停止可能正在运行的HBase服务
    log_info "停止可能正在运行的HBase服务..."
    docker_exec "$HBASE_HOME/bin/stop-hbase.sh" &>/dev/null
    
    # 检查HBase在HDFS上的目录
    log_info "检查HBase在HDFS上的目录..."
    if ! docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -test -d /hbase" &>/dev/null; then
        log_warning "HDFS上不存在HBase目录，尝试创建..."
        if ! docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -mkdir -p /hbase"; then
            log_error "无法在HDFS上创建HBase目录"
            return 1
        fi
        if ! docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -chmod 755 /hbase"; then
            log_warning "无法设置HBase目录权限，但继续执行"
        fi
    fi
    
    # 启动HBase
    log_info "启动HBase服务..."
    if ! docker_exec "$HBASE_HOME/bin/start-hbase.sh"; then
        log_error "HBase服务启动失败"
        return 1
    fi
    
    # 等待HBase服务完全启动
    log_info "等待HBase服务完全启动..."
    sleep 10
    
    # 验证HBase是否正在运行
    if ! docker_exec "ps -ef | grep HMaster | grep -v grep" &>/dev/null; then
        log_error "HBase Master进程未运行，启动可能失败"
        return 1
    else
        log_success "HBase Master进程正在运行"
    fi
    
    # 尝试获取HBase版本信息作为基本测试
    local hbase_version=$(docker_exec "$HBASE_HOME/bin/hbase version" 2>/dev/null)
    if [ -n "$hbase_version" ]; then
        log_success "HBase版本: $hbase_version"
    else
        log_warning "无法获取HBase版本信息，但服务可能仍在运行"
    fi
    
    return 0
}

# 启动Spark服务
start_spark() {
    log_step "3" "启动Spark服务"
    
    # 检查Spark是否已安装
    log_info "检查Spark安装..."
    if ! docker_exec "[ -d \"$SPARK_HOME\" ]"; then
        log_warning "Spark目录不存在: $SPARK_HOME，跳过启动Spark"
        return 0
    fi
    
    # 停止可能正在运行的Spark服务
    log_info "停止可能正在运行的Spark服务..."
    docker_exec "$SPARK_HOME/sbin/stop-all.sh" &>/dev/null
    docker_exec "$SPARK_HOME/sbin/stop-history-server.sh" &>/dev/null
    
    # 检查HDFS是否正在运行（用于日志目录）
    log_info "检查HDFS服务状态..."
    if ! docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -ls /" &>/dev/null; then
        log_warning "HDFS服务未运行，Spark历史服务器日志将无法保存到HDFS"
    else
        # 检查Spark日志目录
        log_info "检查Spark日志目录..."
        if ! docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -test -d /sparklog" &>/dev/null; then
            log_warning "HDFS上不存在Spark日志目录，尝试创建..."
            if ! docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -mkdir -p /sparklog"; then
                log_warning "无法在HDFS上创建Spark日志目录"
            else
                if ! docker_exec_with_env "$HADOOP_HOME/bin/hdfs dfs -chmod 755 /sparklog"; then
                    log_warning "无法设置Spark日志目录权限"
                fi
                log_success "Spark日志目录已创建"
            fi
        fi
    fi
    
    # 启动Spark Master和Worker
    log_info "启动Spark Master和Worker..."
    if ! docker_exec "$SPARK_HOME/sbin/start-all.sh"; then
        log_error "Spark服务启动失败"
        return 1
    fi
    
    # 等待Spark服务完全启动
    log_info "等待Spark服务完全启动..."
    sleep 5
    
    # 验证Spark Master是否正在运行
    if ! docker_exec "ps -ef | grep spark.deploy.master.Master | grep -v grep" &>/dev/null; then
        log_error "Spark Master进程未运行，启动可能失败"
        return 1
    else
        log_success "Spark Master进程正在运行"
    fi
    
    # 验证Spark Worker是否正在运行
    if ! docker_exec "ps -ef | grep spark.deploy.worker.Worker | grep -v grep" &>/dev/null; then
        log_warning "Spark Worker进程未运行，启动可能部分失败"
    else
        local worker_count=$(docker_exec "ps -ef | grep spark.deploy.worker.Worker | grep -v grep | wc -l")
        log_success "Spark Worker进程数: $worker_count"
    fi
    
    # 启动Spark历史服务器
    log_info "启动Spark历史服务器..."
    if ! docker_exec "$SPARK_HOME/sbin/start-history-server.sh"; then
        log_warning "Spark历史服务器启动失败，但继续执行"
    else
        log_success "Spark历史服务器已启动"
    fi
    
    # 验证Spark历史服务器是否正在运行
    if ! docker_exec "ps -ef | grep spark.history.server.HistoryServer | grep -v grep" &>/dev/null; then
        log_warning "Spark历史服务器进程未运行"
    else
        log_success "Spark历史服务器进程正在运行"
    fi
    
    # 显示Spark服务状态
    local spark_status=$(docker_exec "$SPARK_HOME/bin/spark-submit --version 2>&1 | head -2")
    log_success "Spark状态: $spark_status"
    
    return 0
}

# ====================== 主函数 ======================
main() {
    echo -e "${CYAN}"
    echo "  ____               _        _             _   "
    echo " | __ )  ___  ___ __| |_ __ _| |_ _   _ ___| |_ "
    echo " |  _ \\ / _ \\/ __/ _\` | '__| | __| | | / __| __|"
    echo " | |_) | (_) \\__ \\ (_| | |  | | |_| |_| \\__ \\ |_ "
    echo " |____/ \\___/|___/\\__,_|_|  |_|\\__|\\__,_|___/\\__|"
    echo -e "${NC}"
    echo -e "${CYAN}大数据环境启动脚本${NC}\n"
    
    # 初始化日志
    init_log
    log_info "大数据环境启动脚本开始执行..."
    
    # 解析命令行参数
    local start_hadoop_flag=true
    local start_hbase_flag=true
    local start_spark_flag=true
    
    if [ "$#" -gt 0 ]; then
        start_hadoop_flag=false
        start_hbase_flag=false
        start_spark_flag=false
        
        for arg in "$@"; do
            case "$arg" in
                --hadoop)
                    start_hadoop_flag=true
                    ;;
                --hbase)
                    start_hbase_flag=true
                    ;;
                --spark)
                    start_spark_flag=true
                    ;;
                --all)
                    start_hadoop_flag=true
                    start_hbase_flag=true
                    start_spark_flag=true
                    ;;
                *)
                    log_warning "未知参数: $arg，忽略"
                    ;;
            esac
        done
    fi
    
    # 检查Docker容器
    check_docker_container
    docker_status=$?
    
    if [ $docker_status -ne 0 ]; then
        log_error "Docker容器检查失败，脚本无法继续"
        exit 1
    fi
    
    # 启动服务
    if [ "$start_hadoop_flag" = true ]; then
        start_hadoop
        hadoop_status=$?
        if [ $hadoop_status -ne 0 ]; then
            log_error "Hadoop启动失败，可能影响后续组件"
        fi
    else
        log_info "跳过Hadoop启动"
    fi
    
    if [ "$start_hbase_flag" = true ]; then
        start_hbase
        hbase_status=$?
        if [ $hbase_status -ne 0 ]; then
            log_warning "HBase启动过程中出现错误"
        fi
    else
        log_info "跳过HBase启动"
    fi
    
    if [ "$start_spark_flag" = true ]; then
        start_spark
        spark_status=$?
        if [ $spark_status -ne 0 ]; then
            log_warning "Spark启动过程中出现错误"
        fi
    else
        log_info "跳过Spark启动"
    fi
    
    # 获取容器IP
    CONTAINER_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $DOCKER_CONTAINER)
    
    # 总结
    echo -e "\n${GREEN}========================================${NC}"
    echo -e "${GREEN}  大数据环境启动完成！${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo -e "${YELLOW}访问信息：${NC}"
    echo -e "  • 容器名: $DOCKER_CONTAINER"
    echo -e "  • 容器IP: $CONTAINER_IP"
    echo -e "  • NameNode Web UI: http://$CONTAINER_IP:9870"
    echo -e "  • ResourceManager Web UI: http://$CONTAINER_IP:8088"
    echo -e "  • DataNode Web UI: http://$CONTAINER_IP:9864"
    echo -e "  • Spark Master Web UI: http://$CONTAINER_IP:8080"
    echo -e "  • Spark History Server: http://$CONTAINER_IP:18080"
    echo -e "  • HBase Master Web UI: http://$CONTAINER_IP:16010"
    
    log_success "大数据环境启动脚本执行完成！"
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
