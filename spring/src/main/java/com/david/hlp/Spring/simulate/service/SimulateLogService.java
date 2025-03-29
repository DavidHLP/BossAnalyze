package com.david.hlp.Spring.simulate.service;

import org.springframework.stereotype.Service;

import com.david.hlp.Spring.simulate.entity.NginxAccessLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class SimulateLogService {

    private static final Random random = new Random();
    private static final DateTimeFormatter LOG_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z")
        .withZone(ZoneId.of("Asia/Shanghai"));

    private static final String[] VALID_METHODS = {"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH", "TRACE", "CONNECT"};
    private static final String[] VALID_PROTOCOLS = {"HTTP/1.0", "HTTP/1.1", "HTTP/2.0", "HTTP/2", "HTTP/3"};

    /**
     * 检查字符串是否只包含有效的ASCII字符
     */
    private boolean isValidAsciiString(String s) {
        if (s == null || s.isEmpty()) {
            return true;
        }
        try {
            // 检查是否是ASCII且可打印
            for (char c : s.toCharArray()) {
                if (c > 127 || c < 32) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
        /**
     * 清理字符串，移除非ASCII字符
     */
    private String cleanString(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        // 替换转义序列，如 \x22 等
        s = s.replaceAll("\\\\x[0-9a-fA-F]{2}", "");

        // 只保留可打印ASCII字符
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if ((c >= 32 && c <= 126) || c == '\r' || c == '\n' || c == '\t') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 解析单行nginx日志
     *
     * 典型的nginx日志格式:
     * 127.0.0.1 - - [21/Jul/2023:10:30:45 +0800] "GET /index.html HTTP/1.1" 200 1234 "http://example.com/referrer" "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
     */
    private Map<String, Object> parseNginxLog(String logLine , boolean isDefault) {
        if (!isValidAsciiString(logLine) && !isDefault) {
            return null;
        }

        // 将命名捕获组改为简单捕获组，避免语法问题
        String pattern = "([\\d.]+) - - \\[(.*?)\\] \"(.*?)\" (\\d+) (\\d+) \"(.*?)\" \"(.*?)\"";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(logLine);

        if (!m.find()) {
            return null;
        }

        // 通过索引访问捕获组
        String ip = m.group(1);
        String time = m.group(2);
        String request = m.group(3);
        String status = m.group(4);
        String bytes = m.group(5);
        String referrer = m.group(6);
        String userAgent = m.group(7);

        if (!isValidAsciiString(request) || request.contains("\\x")) {
            return null;
        }

        String[] requestParts = request.split(" ");
        if (requestParts.length < 1) {
            return null;
        }

        String method = requestParts.length > 0 ? requestParts[0] : "";
        String path = requestParts.length > 1 ? requestParts[1] : "";
        String protocol = requestParts.length > 2 ? requestParts[2] : "";

        // 验证HTTP方法
        boolean validMethod = false;
        for (String validMethodStr : VALID_METHODS) {
            if (validMethodStr.equals(method)) {
                validMethod = true;
                break;
            }
        }
        if (!validMethod) {
            return null;
        }

        // 验证路径
        if (!isValidAsciiString(path) || path.contains("\\x")) {
            return null;
        }

        // 验证协议
        boolean validProtocol = false;
        for (String validProtocolStr : VALID_PROTOCOLS) {
            if (validProtocolStr.equals(protocol)) {
                validProtocol = true;
                break;
            }
        }
        if (!protocol.isEmpty() && !validProtocol) {
            return null;
        }

        // 确保引用和用户代理没有乱码
        if (!isValidAsciiString(referrer) || !isValidAsciiString(userAgent)) {
            return null;
        }

        Map<String, Object> logData = new HashMap<>();
        logData.put("ip", ip);
        logData.put("time", time);
        logData.put("method", method);
        logData.put("path", cleanString(path));
        logData.put("protocol", protocol);
        logData.put("status", status);
        logData.put("bytes", bytes);
        logData.put("referrer", cleanString(referrer));
        logData.put("user_agent", cleanString(userAgent));

        return logData;
    }

    /**
     * 分析用户行为模式
     * @param parsedLogs 已解析的日志数据列表
     * @return 用户行为模式统计
     */
    private Map<String, Object> analyzeUserBehavior(List<Map<String, Object>> parsedLogs) {
        // 按IP分组，模拟用户会话
        Map<String, List<Map<String, Object>>> sessionsByIp = new HashMap<>();
        Map<String, Integer> pathTransitions = new HashMap<>();

        // 按时间排序日志
        Collections.sort(parsedLogs, Comparator.comparing(log -> (String) log.getOrDefault("time", "")));

        for (Map<String, Object> log : parsedLogs) {
            String ip = (String) log.getOrDefault("ip", "");
            String path = (String) log.getOrDefault("path", "");

            if (!sessionsByIp.containsKey(ip)) {
                sessionsByIp.put(ip, new ArrayList<>());
            }

            sessionsByIp.get(ip).add(log);

            // 统计路径转换
            if (sessionsByIp.get(ip).size() > 1) {
                String prevPath = (String) sessionsByIp.get(ip).get(sessionsByIp.get(ip).size() - 2).getOrDefault("path", "");
                String transitionKey = prevPath + " -> " + path;

                pathTransitions.put(transitionKey, pathTransitions.getOrDefault(transitionKey, 0) + 1);
            }
        }

        // 计算平均会话长度
        double avgSessionLength = 0;
        List<Integer> sessionLengths = sessionsByIp.values().stream()
                .map(List::size)
                .collect(Collectors.toList());

        if (!sessionLengths.isEmpty()) {
            int totalLength = 0;
            for (Integer length : sessionLengths) {
                totalLength += length;
            }
            avgSessionLength = (double) totalLength / sessionLengths.size();
        }
        // 分析常见用户行为路径
        Map<String, Integer> commonPaths = new HashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : sessionsByIp.entrySet()) {
            List<String> pathSequence = entry.getValue().stream()
                    .map(log -> (String) log.getOrDefault("path", ""))
                    .collect(Collectors.toList());
            if (pathSequence.size() > 1) {
                String pathStr = String.join(" -> ", pathSequence);
                commonPaths.put(pathStr, commonPaths.getOrDefault(pathStr, 0) + 1);
            }
        }
        // 提取最常见的行为路径
        List<Map.Entry<String, Integer>> sortedPaths = new ArrayList<>(commonPaths.entrySet());
        Collections.sort(sortedPaths, (a, b) -> b.getValue().compareTo(a.getValue()));
        List<Map.Entry<String, Integer>> topPaths = sortedPaths.size() > 10
                ? sortedPaths.subList(0, 10)
                : sortedPaths;
        // 分析页面停留时间
        Map<String, List<Double>> pageDurations = new HashMap<>();
        for (List<Map<String, Object>> logs : sessionsByIp.values()) {
            for (int i = 0; i < logs.size() - 1; i++) {
                String currentPath = (String) logs.get(i).getOrDefault("path", "");
                // 解析时间
                try {
                    String currentTimeStr = (String) logs.get(i).getOrDefault("time", "");
                    String nextTimeStr = (String) logs.get(i + 1).getOrDefault("time", "");
                    ZonedDateTime currentTime = ZonedDateTime.parse(currentTimeStr, LOG_TIME_FORMATTER);
                    ZonedDateTime nextTime = ZonedDateTime.parse(nextTimeStr, LOG_TIME_FORMATTER);
                    double duration = (nextTime.toEpochSecond() - currentTime.toEpochSecond());
                    if (!pageDurations.containsKey(currentPath)) {
                        pageDurations.put(currentPath, new ArrayList<>());
                    }
                    // 只统计合理的停留时间（少于30分钟）
                    if (duration > 0 && duration < 1800) {
                        pageDurations.get(currentPath).add(duration);
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        }
        // 计算平均停留时间
        Map<String, Double> avgPageDurations = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : pageDurations.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                double sum = 0;
                for (Double duration : entry.getValue()) {
                    sum += duration;
                }
                avgPageDurations.put(entry.getKey(), sum / entry.getValue().size());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("avg_session_length", avgSessionLength);
        result.put("top_paths", topPaths);
        result.put("path_transitions", pathTransitions);
        result.put("avg_page_durations", avgPageDurations);
        return result;
    }
    /**
     * 过滤掉包含无效字符的元素
     */
    private List<String> filterValidElements(List<String> elements) {
        return elements.stream()
                .filter(e -> isValidAsciiString(e) && !e.contains("\\x"))
                .collect(Collectors.toList());
    }
    /**
     * 生成单个用户会话
     */
    private List<Map<String, Object>> generateUserSession(
            Map<String, Object> behaviorData,
            List<String> ips,
            List<String> paths,
            List<String> userAgents,
            List<String> referrers,
            List<String> methods,
            List<String> protocols,
            List<String> statuses) {
        // 过滤有效元素
        List<String> validIps = filterValidElements(ips);
        List<String> validPaths = filterValidElements(paths);
        List<String> validUserAgents = filterValidElements(userAgents);
        List<String> validReferrers = filterValidElements(referrers);
        List<String> validMethods = new ArrayList<>();
        for (String method : VALID_METHODS) {
            validMethods.add(method);
        }
        List<String> validProtocols = new ArrayList<>();
        for (String protocol : VALID_PROTOCOLS) {
            validProtocols.add(protocol);
        }
        List<String> validStatuses = new ArrayList<>();
        validStatuses.add("200");
        validStatuses.add("301");
        validStatuses.add("302");
        validStatuses.add("304");
        validStatuses.add("400");
        validStatuses.add("401");
        validStatuses.add("403");
        validStatuses.add("404");
        validStatuses.add("500");
        validStatuses.add("502");
        validStatuses.add("503");
        // 随机选择一个IP
        String ip = validIps.isEmpty() ? "127.0.0.1" : validIps.get(random.nextInt(validIps.size()));
        // 随机选择会话长度，基于真实数据的分布
        double avgLength = (double) behaviorData.getOrDefault("avg_session_length", 3.0);
        int sessionLength = Math.max(2, (int) (random.nextGaussian() * (avgLength/3) + avgLength));
        // 随机选择用户代理（浏览器）
        String userAgent = validUserAgents.isEmpty()
                ? "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
                : validUserAgents.get(random.nextInt(validUserAgents.size()));
        // 生成用户ID (模拟)
        String userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
        // 随机选择一个常见的行为路径或随机生成路径
        List<String> pathSequence = new ArrayList<>();
        // 有50%的概率使用常见路径
        @SuppressWarnings("unchecked")
        List<Map.Entry<String, Integer>> topPaths = (List<Map.Entry<String, Integer>>) behaviorData.getOrDefault("top_paths", new ArrayList<>());
        if (!topPaths.isEmpty() && random.nextDouble() < 0.5) {
            // 随机选择一个常见路径
            Map.Entry<String, Integer> pathEntry = topPaths.get(random.nextInt(topPaths.size()));
            String pathStr = pathEntry.getKey();
            // 分割路径序列
            String[] paths1 = pathStr.split(" -> ");
            // 清理路径序列，确保没有乱码
            for (String p : paths1) {
                if (isValidAsciiString(p)) {
                    pathSequence.add(cleanString(p));
                }
            }
        } else {
            // 生成随机路径序列
            String currentPath = validPaths.isEmpty() ? "/" : validPaths.get(random.nextInt(validPaths.size()));
            pathSequence.add(currentPath);
            // 使用转换概率生成剩余的路径
            @SuppressWarnings("unchecked")
            Map<String, Integer> transitions = (Map<String, Integer>) behaviorData.getOrDefault("path_transitions", new HashMap<>());
            Map<String, Integer> validTransitions = new HashMap<>();
            for (Map.Entry<String, Integer> entry : transitions.entrySet()) {
                if (isValidAsciiString(entry.getKey())) {
                    validTransitions.put(entry.getKey(), entry.getValue());
                }
            }
            for (int i = 0; i < sessionLength - 1; i++) {
                // 尝试找到一个合理的下一个路径
                String nextPath = null;
                for (String key : validTransitions.keySet()) {
                    if (key.startsWith(currentPath + " -> ")) {
                        String candidatePath = key.split(" -> ")[1];
                        if (isValidAsciiString(candidatePath)) {
                            nextPath = candidatePath;
                            break;
                        }
                    }
                }
                // 如果找不到合理的转换，随机选择
                if (nextPath == null) {
                    nextPath = validPaths.isEmpty() ? "/" : validPaths.get(random.nextInt(validPaths.size()));
                }
                pathSequence.add(nextPath);
                currentPath = nextPath;
            }
        }
        // 确保路径序列不为空
        if (pathSequence.isEmpty()) {
            pathSequence.add("/");
        }
        // 实际会话长度为路径序列长度
        sessionLength = pathSequence.size();
        // 生成会话开始时间（随机时间）
        LocalDateTime now = LocalDateTime.now();
        int timeOffset = random.nextInt(172800) - 86400; // 前后24小时内
        LocalDateTime sessionStart = now.plusSeconds(timeOffset);
        // 生成会话日志
        List<Map<String, Object>> sessionLogs = new ArrayList<>();
        LocalDateTime currentTime = sessionStart;
        for (int i = 0; i < pathSequence.size(); i++) {
            String path = pathSequence.get(i);
            // 随机选择HTTP方法，GET占比更高
            String method = random.nextDouble() < 0.85 ? "GET" : validMethods.get(random.nextInt(validMethods.size()));
            // 随机选择协议
            String protocol = validProtocols.get(random.nextInt(validProtocols.size()));
            // 随机选择状态码，大部分是200
            String status = random.nextDouble() < 0.9 ? "200" : validStatuses.get(random.nextInt(validStatuses.size()));
            // 引用来源，第一个请求通常是直接访问或外部引用
            String referrer = "-";
            if (i > 0) {
                referrer = "http://example.com" + pathSequence.get(i - 1);
            } else if (!validReferrers.isEmpty() && random.nextDouble() < 0.3) { // 30%的可能有外部引用
                referrer = validReferrers.get(random.nextInt(validReferrers.size()));
            }
            // 确保引用不含乱码
            referrer = cleanString(referrer);
            // 随机生成响应大小
            String bytesSent = String.valueOf(random.nextInt(9900) + 100);
            // 生成日志
            Map<String, Object> log = new HashMap<>();
            log.put("ip", ip);
            log.put("time", currentTime.atZone(ZoneOffset.ofHours(8)).format(LOG_TIME_FORMATTER));
            log.put("method", method);
            log.put("path", cleanString(path));
            log.put("protocol", protocol);
            log.put("status", status);
            log.put("bytes", bytesSent);
            log.put("referrer", referrer);
            log.put("user_agent", cleanString(userAgent));
            log.put("user_id", userId); // 添加用户ID
            sessionLogs.add(log);
            // 更新时间，模拟页面停留
            @SuppressWarnings("unchecked")
            Map<String, Double> pageDurations = (Map<String, Double>) behaviorData.getOrDefault("avg_page_durations", new HashMap<>());
            double duration;
            if (pageDurations.containsKey(path)) {
                double avgDuration = pageDurations.get(path);
                // 使用正态分布模拟随机但合理的停留时间
                duration = Math.max(1, random.nextGaussian() * (avgDuration/4) + avgDuration);
            } else {
                // 默认停留时间5-60秒
                duration = random.nextInt(56) + 5;
            }
            currentTime = currentTime.plusSeconds((long) duration);
        }
        return sessionLogs;
    }
    /**
     * 基于已解析的日志生成模拟用户行为日志数据
     */
    private List<String> generateMockLogData(List<Map<String, Object>> parsedLogs, int count) {
        if (parsedLogs == null || parsedLogs.isEmpty()) {
            return new ArrayList<>();
        }
        // 过滤掉包含乱码的日志
        List<Map<String, Object>> cleanLogs = new ArrayList<>();
        for (Map<String, Object> log : parsedLogs) {
            if (log != null) {
                boolean isClean = true;
                for (Object value : log.values()) {
                    if (!isValidAsciiString(String.valueOf(value))) {
                        isClean = false;
                        break;
                    }
                }
                if (isClean) {
                    cleanLogs.add(log);
                }
            }
        }

        log.info("过滤后剩余 {} 行有效日志数据", cleanLogs.size());

        if (cleanLogs.isEmpty()) {
            log.warn("警告：所有日志都被过滤掉了，无法生成数据");
            return new ArrayList<>();
        }

        // 分析用户行为
        Map<String, Object> behaviorData = analyzeUserBehavior(cleanLogs);
        // 从真实数据中提取唯一的元素用于随机选择
        Set<String> ipSet = new HashSet<>();
        Set<String> methodSet = new HashSet<>();
        Set<String> pathSet = new HashSet<>();
        Set<String> protocolSet = new HashSet<>();
        Set<String> statusSet = new HashSet<>();
        Set<String> userAgentSet = new HashSet<>();
        Set<String> referrerSet = new HashSet<>();
        for (Map<String, Object> log : cleanLogs) {
            if (log.containsKey("ip")) ipSet.add((String) log.get("ip"));
            if (log.containsKey("method")) methodSet.add((String) log.get("method"));
            if (log.containsKey("path")) pathSet.add((String) log.get("path"));
            if (log.containsKey("protocol")) protocolSet.add((String) log.get("protocol"));
            if (log.containsKey("status")) statusSet.add((String) log.get("status"));
            if (log.containsKey("user_agent")) userAgentSet.add((String) log.get("user_agent"));
            if (log.containsKey("referrer")) referrerSet.add((String) log.get("referrer"));
        }

        List<String> ips = new ArrayList<>(ipSet);
        List<String> methods = new ArrayList<>(methodSet);
        List<String> paths = new ArrayList<>(pathSet);
        List<String> protocols = new ArrayList<>(protocolSet);
        List<String> statuses = new ArrayList<>(statusSet);
        List<String> userAgents = new ArrayList<>(userAgentSet);
        List<String> referrers = new ArrayList<>(referrerSet);

        // 过滤掉包含乱码的元素
        ips = filterValidElements(ips);
        paths = filterValidElements(paths);
        userAgents = filterValidElements(userAgents);
        referrers = filterValidElements(referrers);

        // 生成用户会话
        List<Map<String, Object>> allSessionLogs = new ArrayList<>();
        int generatedLines = 0;

        while (generatedLines < count) {
            List<Map<String, Object>> sessionLogs = generateUserSession(
                behaviorData, ips, paths, userAgents, referrers, methods, protocols, statuses
            );
            allSessionLogs.addAll(sessionLogs);
            generatedLines += sessionLogs.size();
        }

        // 按时间排序所有日志
        Collections.sort(allSessionLogs, Comparator.comparing(x -> (String) x.get("time")));

        // 截断到请求的行数
        if (allSessionLogs.size() > count) {
            allSessionLogs = allSessionLogs.subList(0, count);
        }

        // 转换为Nginx日志格式
        List<String> mockData = new ArrayList<>();
        for (Map<String, Object> log : allSessionLogs) {
            String logLine = String.format("%s - - [%s] \"%s %s %s\" %s %s \"%s\" \"%s\"",
                    log.get("ip"),
                    log.get("time"),
                    log.get("method"),
                    log.get("path"),
                    log.get("protocol"),
                    log.get("status"),
                    log.get("bytes"),
                    log.get("referrer"),
                    log.get("user_agent"));
            mockData.add(logLine);
        }

        return mockData;
    }

    /**
     * 生成模拟日志数据并保存到文件
     */
    private List<NginxAccessLog> generateAndSaveMockData(int mockCount) {
        try {
            // 读取真实日志文件
            List<String> data = new ArrayList<>();
            File logDir = new File("/bigdata/project/nginx_logs/");
            File[] logFiles = logDir.listFiles((dir, name) -> name.endsWith(".log"));

            log.info("共读取 {} 个日志文件", (logFiles != null ? logFiles.length : 0));

            if (logFiles != null) {
                for (File logFile : logFiles) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            data.add(line);
                        }
                    } catch (Exception e) {
                        log.error("读取文件 {} 时出错: {}", logFile.getName(), e.getMessage());
                    }
                }
            }

            log.info("共读取 {} 行日志数据", data.size());

            // 如果没有读取到数据，返回空列表
            if (data.isEmpty()) {
                log.warn("未能读取到任何日志数据，无法生成模拟数据");
                return Collections.emptyList();
            }

            // 解析日志
            List<Map<String, Object>> parsedLogs = new ArrayList<>();
            for (String logLine : data) {
                Map<String, Object> parsedLog = parseNginxLog(logLine.trim(), false);
                if (parsedLog != null) {
                    parsedLogs.add(parsedLog);
                }
            }

            log.info("成功解析 {} 行日志数据", parsedLogs.size());

            // 如果解析结果为空，返回空列表
            if (parsedLogs.isEmpty()) {
                log.warn("未能解析出有效的日志数据，无法生成模拟数据");
                return Collections.emptyList();
            }

            // 使用格式化的JSON输出模拟数据的详细信息
            Map<String, Object> firstLog = parsedLogs.get(0);
            StringBuilder formattedLog = new StringBuilder("模拟数据详细:\n{\n");
            for (Map.Entry<String, Object> entry : firstLog.entrySet()) {
                formattedLog.append(String.format("  %s: %s\n", entry.getKey(), entry.getValue()));
            }
            formattedLog.append("}");
            log.info(formattedLog.toString());

            // 生成模拟日志数据
            List<String> mockLogData = generateMockLogData(parsedLogs, mockCount);
            // 如果生成结果为空，返回空列表
            if (mockLogData.isEmpty()) {
                log.warn("无法根据真实数据生成模拟日志，无法创建模拟数据");
                return Collections.emptyList();
            }

            // 转换为NginxAccessLog实体并返回
            List<NginxAccessLog> result = new ArrayList<>();
            for (String mockLog : mockLogData) {
                Map<String, Object> logData = parseNginxLog(mockLog, true);
                if (logData != null) {
                    NginxAccessLog accessLog = NginxAccessLog.builder()
                            .ip((String) logData.get("ip"))
                            .accessTime((String) logData.get("time"))
                            .method((String) logData.get("method"))
                            .path((String) logData.get("path"))
                            .protocol((String) logData.get("protocol"))
                            .status((String) logData.get("status"))
                            .bytes((String) logData.get("bytes"))
                            .referrer((String) logData.get("referrer"))
                            .userAgent((String) logData.get("user_agent"))
                            .userId((String) logData.getOrDefault("user_id", "user_" + UUID.randomUUID().toString().substring(0, 8)))
                            .build();
                    result.add(accessLog);
                }
            }

            log.info("成功生成 {} 条模拟日志记录", result.size());

            return result;
        } catch (Exception e) {
            log.error("生成模拟数据时出错: {}", e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
    public List<NginxAccessLog> generateMockLogs(int mockCount) {
        return generateAndSaveMockData(mockCount);
    }
}
