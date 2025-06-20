#!/bin/bash

# =================================================================
# Hadoop异常数据块恢复与清除脚本
# 用于Docker容器hadoop-single
# 作者: David's AI Assistant
# =================================================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置参数
CONTAINER_NAME="hadoop-single"
LOG_FILE="/tmp/hadoop-recovery-$(date +%Y%m%d_%H%M%S).log"
HDFS_USER="hdfs"

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

log_debug() {
    echo -e "${BLUE}[DEBUG]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

# 检查Docker容器是否运行
check_container() {
    log_info "检查Docker容器 $CONTAINER_NAME 状态..."
    if ! docker ps | grep -q "$CONTAINER_NAME"; then
        log_error "容器 $CONTAINER_NAME 未运行或不存在"
        return 1
    fi
    log_info "容器 $CONTAINER_NAME 运行正常"
    return 0
}

# 执行容器内命令
exec_in_container() {
    local cmd="$1"
    log_debug "执行容器命令: $cmd"
    docker exec "$CONTAINER_NAME" bash -c "$cmd"
}

# 执行容器内交互命令
exec_in_container_interactive() {
    local cmd="$1"
    log_debug "执行容器交互命令: $cmd"
    docker exec -it "$CONTAINER_NAME" bash -c "$cmd"
}

# 检查HDFS状态
check_hdfs_status() {
    log_info "检查HDFS状态..."
    exec_in_container "hdfs dfsadmin -report" | tee -a "$LOG_FILE"
    
    # 检查安全模式
    local safemode_status=$(exec_in_container "hdfs dfsadmin -safemode get" 2>/dev/null)
    if echo "$safemode_status" | grep -q "ON"; then
        log_warn "HDFS当前处于安全模式"
        return 1
    fi
    log_info "HDFS状态正常"
    return 0
}

# 查找损坏的数据块
find_corrupted_blocks() {
    log_info "扫描损坏的数据块..."
    local corrupted_blocks_file="/tmp/corrupted_blocks.txt"
    
    # 生成HDFS fsck报告
    exec_in_container "hdfs fsck / -files -blocks -locations" > "$corrupted_blocks_file" 2>&1
    
    # 提取损坏的块信息
    grep -E "(CORRUPT|MISSING)" "$corrupted_blocks_file" > "/tmp/corrupted_summary.txt" 2>/dev/null
    
    if [ -s "/tmp/corrupted_summary.txt" ]; then
        log_warn "发现损坏的数据块:"
        cat "/tmp/corrupted_summary.txt" | tee -a "$LOG_FILE"
        return 0
    else
        log_info "未发现损坏的数据块"
        return 1
    fi
}

# 查找副本不足的数据块
find_under_replicated_blocks() {
    log_info "检查副本不足的数据块..."
    local under_rep_info=$(exec_in_container "hdfs dfsadmin -metasave /tmp/metasave.out && cat /tmp/metasave.out | grep 'Under replicated'" 2>/dev/null)
    
    if [ -n "$under_rep_info" ]; then
        log_warn "发现副本不足的数据块:"
        echo "$under_rep_info" | tee -a "$LOG_FILE"
        return 0
    else
        log_info "所有数据块副本数正常"
        return 1
    fi
}

# 查找过度复制的数据块
find_over_replicated_blocks() {
    log_info "检查过度复制的数据块..."
    local over_rep_info=$(exec_in_container "hdfs dfsadmin -metasave /tmp/metasave.out && cat /tmp/metasave.out | grep 'Over replicated'" 2>/dev/null)
    
    if [ -n "$over_rep_info" ]; then
        log_warn "发现过度复制的数据块:"
        echo "$over_rep_info" | tee -a "$LOG_FILE"
        return 0
    else
        log_info "所有数据块副本数正常"
        return 1
    fi
}

# 修复副本不足的数据块
repair_under_replicated() {
    log_info "开始修复副本不足的数据块..."
    
    # 获取datanode地址
    local datanode_addr=$(exec_in_container "hdfs dfsadmin -report 2>/dev/null | grep 'Name:' | head -1 | awk '{print \$2}'" 2>/dev/null | tail -1)
    if [ -n "$datanode_addr" ] && [ "$datanode_addr" != "" ]; then
        log_info "触发数据节点块报告: $datanode_addr"
        exec_in_container "hdfs dfsadmin -triggerBlockReport $datanode_addr" 2>/dev/null || log_warn "块报告触发失败，可能是权限或地址问题"
    else
        log_warn "无法获取datanode地址，跳过块报告触发"
    fi
    sleep 5
    
    # 运行平衡器来重新分配块
    log_info "启动数据块平衡器..."
    exec_in_container "nohup hdfs balancer -threshold 10 > /tmp/balancer.log 2>&1 &"
    
    log_info "副本修复任务已启动，请等待完成"
}

# 清理损坏的数据块
clean_corrupted_blocks() {
    log_warn "准备清理损坏的数据块..."
    
    # 获取损坏文件列表
    local corrupted_files=$(exec_in_container "hdfs fsck / -list-corruptfileblocks" 2>/dev/null | grep -v "^Connecting" | grep -v "^FSCK" | grep "^/")
    
    if [ -n "$corrupted_files" ]; then
        echo "发现以下损坏文件:"
        echo "$corrupted_files"
        
        # 在非交互模式下直接确认，或提供交互选择
        if [ -t 0 ]; then
            read -p "确认删除这些损坏文件? (y/N): " confirm
        else
            log_warn "非交互模式，自动确认删除损坏文件"
            confirm="y"
        fi
        
        if [[ $confirm =~ ^[Yy]$ ]]; then
            while IFS= read -r file; do
                if [ -n "$file" ]; then
                    log_warn "删除损坏文件: $file"
                    exec_in_container "hdfs dfs -rm -r -skipTrash '$file'"
                fi
            done <<< "$corrupted_files"
            log_info "损坏文件清理完成"
        else
            log_info "取消清理操作"
        fi
    else
        log_info "未找到需要清理的损坏文件"
    fi
}

# 修复过度复制的数据块
fix_over_replicated() {
    log_info "开始修复过度复制的数据块..."
    
    # 获取datanode地址
    local datanode_addr=$(exec_in_container "hdfs dfsadmin -report 2>/dev/null | grep 'Name:' | head -1 | awk '{print \$2}'" 2>/dev/null | tail -1)
    
    # Hadoop会自动处理过度复制，我们触发块报告来加速处理
    if [ -n "$datanode_addr" ] && [ "$datanode_addr" != "" ]; then
        log_info "触发数据节点块报告: $datanode_addr"
        exec_in_container "hdfs dfsadmin -triggerBlockReport $datanode_addr" 2>/dev/null || log_warn "块报告触发失败，可能是权限或地址问题"
    else
        log_warn "无法获取datanode地址，跳过块报告触发"
    fi
    
    # 设置平衡器带宽来优化处理速度
    exec_in_container "hdfs dfsadmin -setBalancerBandwidth 1048576"
    
    log_info "过度复制修复任务已触发"
}

# 进入安全模式
enter_safe_mode() {
    log_info "进入HDFS安全模式..."
    exec_in_container "hdfs dfsadmin -safemode enter"
}

# 退出安全模式
leave_safe_mode() {
    log_info "退出HDFS安全模式..."
    exec_in_container "hdfs dfsadmin -safemode leave"
}

# 强制退出安全模式
force_leave_safe_mode() {
    log_warn "强制退出HDFS安全模式..."
    exec_in_container "hdfs dfsadmin -safemode forceExit"
}

# 备份HDFS元数据
backup_metadata() {
    log_info "备份HDFS元数据..."
    local backup_dir="/tmp/hdfs_backup_$(date +%Y%m%d_%H%M%S)"
    
    exec_in_container "mkdir -p $backup_dir"
    exec_in_container "hdfs dfsadmin -metasave $backup_dir/metasave.out"
    exec_in_container "hdfs dfsadmin -printTopology > $backup_dir/topology.out"
    exec_in_container "hdfs dfsadmin -report > $backup_dir/cluster_report.out"
    
    log_info "元数据已备份到容器内: $backup_dir"
}

# 监控修复进度
monitor_repair_progress() {
    log_info "监控修复进度..."
    local max_wait=300  # 最多等待5分钟
    local wait_time=0
    
    while [ $wait_time -lt $max_wait ]; do
        local under_rep=$(exec_in_container "hdfs dfsadmin -report 2>/dev/null | grep 'Under replicated blocks:' | awk '{print \$4}'" 2>/dev/null | tail -1)
        local over_rep=$(exec_in_container "hdfs dfsadmin -report 2>/dev/null | grep 'Over replicated blocks:' | awk '{print \$4}'" 2>/dev/null | tail -1)
        
        log_info "当前状态 - 副本不足: ${under_rep:-0}, 过度复制: ${over_rep:-0}"
        
        if [ "${under_rep:-0}" = "0" ] && [ "${over_rep:-0}" = "0" ]; then
            log_info "所有数据块副本状态已正常！"
            break
        fi
        
        sleep 30
        wait_time=$((wait_time + 30))
    done
    
    if [ $wait_time -ge $max_wait ]; then
        log_warn "监控超时，请手动检查修复状态"
    fi
}

# 显示帮助信息
show_help() {
    cat << EOF
=================================================================
Hadoop异常数据块恢复与清除脚本
=================================================================

用法: $0 [选项]

选项:
    -h, --help              显示帮助信息
    -c, --check             检查HDFS状态和异常块
    -r, --repair            修复副本不足的数据块
    -C, --clean             清理损坏的数据块
    -o, --over-rep          修复过度复制的数据块
    -s, --safe-enter        进入安全模式
    -S, --safe-leave        退出安全模式
    -f, --force-safe        强制退出安全模式
    -b, --backup            备份HDFS元数据
    -m, --monitor           监控修复进度
    -a, --auto              自动检查并修复所有问题

示例:
    $0 --check              # 检查状态
    $0 --repair             # 修复副本不足
    $0 --clean              # 清理损坏块
    $0 --auto               # 自动修复

注意: 请确保Docker容器 '$CONTAINER_NAME' 正在运行
EOF
}

# 自动修复功能
auto_repair() {
    log_info "开始自动检查和修复..."
    
    # 检查容器状态
    if ! check_container; then
        return 1
    fi
    
    # 备份元数据
    backup_metadata
    
    # 检查HDFS状态
    if ! check_hdfs_status; then
        log_warn "HDFS可能处于异常状态，尝试修复..."
        force_leave_safe_mode
        sleep 5
    fi
    
    # 检查并修复各种异常
    local need_repair=false
    
    if find_corrupted_blocks; then
        clean_corrupted_blocks
        need_repair=true
    fi
    
    if find_under_replicated_blocks; then
        repair_under_replicated
        need_repair=true
    fi
    
    if find_over_replicated_blocks; then
        fix_over_replicated
        need_repair=true
    fi
    
    # 如果执行了修复操作，监控进度
    if [ "$need_repair" = true ]; then
        log_info "开始监控修复进度..."
        sleep 10  # 等待修复任务启动
        monitor_repair_progress
    fi
    
    log_info "自动修复完成，请检查日志: $LOG_FILE"
}

# 主函数
main() {
    echo -e "${BLUE}==================================================================${NC}"
    echo -e "${BLUE}Hadoop异常数据块恢复与清除脚本${NC}"
    echo -e "${BLUE}容器: $CONTAINER_NAME${NC}"
    echo -e "${BLUE}日志文件: $LOG_FILE${NC}"
    echo -e "${BLUE}==================================================================${NC}"
    
    # 参数解析
    case "$1" in
        -h|--help)
            show_help
            ;;
        -c|--check)
            check_container && check_hdfs_status
            find_corrupted_blocks
            find_under_replicated_blocks
            find_over_replicated_blocks
            ;;
        -r|--repair)
            check_container && repair_under_replicated
            ;;
        -C|--clean)
            check_container && clean_corrupted_blocks
            ;;
        -o|--over-rep)
            check_container && fix_over_replicated
            ;;
        -s|--safe-enter)
            check_container && enter_safe_mode
            ;;
        -S|--safe-leave)
            check_container && leave_safe_mode
            ;;
        -f|--force-safe)
            check_container && force_leave_safe_mode
            ;;
        -b|--backup)
            check_container && backup_metadata
            ;;
        -m|--monitor)
            check_container && monitor_repair_progress
            ;;
        -a|--auto)
            auto_repair
            ;;
        ""|*)
            echo "未指定操作参数，显示帮助信息:"
            echo ""
            show_help
            ;;
    esac
    
    echo -e "\n${GREEN}操作完成！日志保存在: $LOG_FILE${NC}"
}

# 脚本入口
main "$@"
