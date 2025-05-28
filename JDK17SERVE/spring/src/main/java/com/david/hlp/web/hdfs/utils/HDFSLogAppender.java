package com.david.hlp.web.hdfs.utils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * HDFS日志追加器
 * 将Logback日志事件写入HDFS
 */
public class HDFSLogAppender extends AppenderBase<ILoggingEvent> {

    private FileSystem fileSystem;
    private String logDir;
    private String datePattern = "yyyy-MM-dd";
    private FSDataOutputStream currentOutputStream;
    private String currentDate;
    private String processId;

    public HDFSLogAppender(FileSystem fileSystem, String logDir) {
        this.fileSystem = fileSystem;
        this.logDir = logDir;
        this.processId = System.getProperty("PID", "?");
    }

    @Override
    public void start() {
        if (fileSystem == null) {
            addError("FileSystem不能为空");
            return;
        }
        if (logDir == null || logDir.isEmpty()) {
            addError("日志目录不能为空");
            return;
        }
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted()) {
            return;
        }

        try {
            String formattedDate = new SimpleDateFormat(datePattern).format(new Date());

            // 如果日期变化了或者输出流未初始化，创建新的输出流
            if (currentOutputStream == null || !formattedDate.equals(currentDate)) {
                closeCurrentStream();

                currentDate = formattedDate;
                String fileName = currentDate + ".log";

                // 确保目录存在
                Path dirPath = new Path(logDir);
                if (!fileSystem.exists(dirPath)) {
                    fileSystem.mkdirs(dirPath);
                }

                // 构建完整日志路径
                Path logFilePath = new Path(logDir + "/" + fileName);

                // 打开或创建日志文件
                if (fileSystem.exists(logFilePath)) {
                    currentOutputStream = fileSystem.append(logFilePath);
                } else {
                    currentOutputStream = fileSystem.create(logFilePath);
                }
            }

            // 格式化日志条目，与Logback标准格式一致
            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String timestamp = timestampFormat.format(new Date(event.getTimeStamp()));

            String logLevel = event.getLevel().toString();
            String threadName = event.getThreadName();
            String loggerName = event.getLoggerName();
            String message = event.getFormattedMessage();

            // 标准Logback格式：时间 级别 PID --- [线程] 类名 : 消息
            String fullLogEntry = String.format("%s %s %s --- [%s] %s : %s%n",
                    timestamp,
                    logLevel,
                    processId,
                    threadName,
                    loggerName,
                    message);

            currentOutputStream.write(fullLogEntry.getBytes(StandardCharsets.UTF_8));
            currentOutputStream.hsync(); // 确保数据写入HDFS

        } catch (IOException e) {
            addError("写入日志到HDFS失败", e);
        }
    }

    private void closeCurrentStream() {
        if (currentOutputStream != null) {
            try {
                currentOutputStream.close();
            } catch (IOException e) {
                addError("关闭HDFS输出流失败", e);
            } finally {
                currentOutputStream = null;
            }
        }
    }

    @Override
    public void stop() {
        closeCurrentStream();
        super.stop();
    }
}