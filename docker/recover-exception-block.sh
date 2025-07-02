#!/bin/bash

# =================================================================
# Hadoop异常数据块恢复与清除脚本
# 用于Docker容器hadoop-single
# 作者: David's AI Assistant
# =================================================================

CONTAINER_NAME="hadoop-single"

# 函数：检查指定的Docker容器是否正在运行
check_container() {
    # 使用docker ps命令查找具有指定名称的正在运行的容器
    if [ ! "$(docker ps -q -f name=$CONTAINER_NAME)" ]; then
        # 如果找不到，则打印错误消息并以状态1退出
        echo "错误：'$CONTAINER_NAME' 容器未运行。脚本终止。"
        exit 1
    fi
    echo "确认：'$CONTAINER_NAME' 容器正在运行。"
}

# 函数：在指定的容器内执行命令
# 参数: 要在容器内执行的命令
exec_in_container() {
    docker exec "$CONTAINER_NAME" "$@"
}

# 函数：检查HDFS路径的健康状况
# 参数: HDFS路径
check_health() {
    local path=$1
    echo "正在对HDFS路径 '$path' 进行健康状况评估..."
    # 执行hdfs fsck命令并捕获输出
    local output
    output=$(exec_in_container hdfs fsck "$path" 2>&1) # Redirect stderr to stdout
    echo "$output"

    # 检查输出中是否包含特定字符串以确定健康状态
    if echo "$output" | grep -q "Status: HEALTHY"; then
        echo "评估完成：路径 '$path' 状态正常。"
        return 0 # 0 表示健康
    elif echo "$output" | grep -q "CORRUPT"; then
        echo "警告：检测到路径 '$path' 存在损坏的数据块或文件。"
        return 1 # 1 表示损坏
    else
        echo "信息：路径 '$path' 状态未知，未检测到明确的损坏。"
        return 2 # 2 表示状态未知但未报告损坏
    fi
}

# 主函数
main() {
    # 检查容器是否运行
    check_container

    # 提示用户输入HDFS路径
    read -p "请输入待处理的HDFS目录路径 (例如 /user/hive/warehouse): " hdfs_path

    # 检查用户是否输入路径
    if [ -z "$hdfs_path" ]; then
        echo "错误：未指定HDFS路径。操作已取消。"
        exit 1
    fi

    # 步骤 1: 检查初始健康状况
    check_health "$hdfs_path"
    local health_status=$?

    # 如果路径健康，则脚本完成
    if [ $health_status -eq 0 ]; then
        echo "系统状态正常，无需进一步操作。"
        exit 0
    fi

    # 如果检测到损坏
    if [ $health_status -eq 1 ]; then
        # 步骤 2: 尝试修复 (移动损坏的文件)
        echo "检测到数据损坏。正在启动自动修复程序..."
        echo "操作：正在尝试将损坏文件隔离至 /lost+found 目录。"
        exec_in_container hdfs fsck "$hdfs_path" -move

        # 步骤 3: 修复后再次检查
        echo "隔离操作完成。正在重新进行健康状况评估。"
        check_health "$hdfs_path"
        local health_status_after_move=$?

        if [ $health_status_after_move -eq 1 ]; then
            # 步骤 4: 如果仍然损坏, 则删除
            echo "警告：隔离操作未能解决问题。将执行数据清理操作。"
            echo "操作：正在删除无法修复的损坏文件。"
            exec_in_container hdfs fsck "$hdfs_path" -delete

            echo "清理操作完成。正在执行最终健康状况评估。"
            check_health "$hdfs_path"
            local final_health_status=$?

            if [ $final_health_status -eq 0 ]; then
                echo "成功：路径 '$hdfs_path' 已恢复至正常状态。"
            else
                echo "错误：清理操作失败。问题依然存在，建议进行手动干预。"
            fi
        elif [ $health_status_after_move -eq 0 ]; then
            echo "成功：通过隔离损坏文件，已解决问题。"
        else
            echo "信息：隔离操作后，未检测到明确的数据损坏。"
        fi
    fi
}

# 运行主函数
main