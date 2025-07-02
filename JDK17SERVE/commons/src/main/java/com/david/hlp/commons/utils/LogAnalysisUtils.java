package com.david.hlp.commons.utils;

import com.david.hlp.commons.entity.logs.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志分析工具类
 * 提供日志分析相关的通用工具方法
 */
public final class LogAnalysisUtils {

    private LogAnalysisUtils() {
        // 工具类，禁止实例化
    }

    // 日期时间格式化器
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH");
    private static final DateTimeFormatter MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // 浏览器版本提取正则表达式
    private static final Map<String, Pattern> BROWSER_VERSION_PATTERNS = Map.of(
            "Chrome", Pattern.compile("Chrome/([\\d.]+)"),
            "Firefox", Pattern.compile("Firefox/([\\d.]+)"),
            "Safari", Pattern.compile("Version/([\\d.]+).*Safari"),
            "Edge", Pattern.compile("Edg/([\\d.]+)"),
            "Opera", Pattern.compile("OPR/([\\d.]+)"),
            "IE", Pattern.compile("MSIE ([\\d.]+)|rv:([\\d.]+)"));

    // 操作系统检测正则表达式
    private static final Map<String, Pattern> OS_PATTERNS = Map.of(
            "Windows", Pattern.compile("Windows NT ([\\d.]+)"),
            "macOS", Pattern.compile("Mac OS X ([\\d_]+)"),
            "Linux", Pattern.compile("Linux"),
            "Android", Pattern.compile("Android ([\\d.]+)"),
            "iOS", Pattern.compile("OS ([\\d_]+).*like Mac OS X"));

    // 设备类型检测关键词
    private static final Map<String, List<String>> DEVICE_TYPE_KEYWORDS = Map.of(
            "Mobile", Arrays.asList("Mobile", "Android", "iPhone", "iPod"),
            "Tablet", Arrays.asList("iPad", "Tablet"),
            "Desktop", Arrays.asList("Windows", "Mac", "Linux", "X11"));

    /**
     * 从User-Agent中提取浏览器信息
     *
     * @param userAgent User-Agent字符串
     * @return 浏览器信息
     */
    public static BrowserStatistics.BrowserInfo extractBrowserInfo(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return createDefaultBrowserInfo();
        }

        String browser = detectBrowser(userAgent);
        String version = extractBrowserVersion(userAgent, browser);
        String os = detectOperatingSystem(userAgent);
        String deviceType = detectDeviceType(userAgent);

        Map<String, Integer> osDistribution = new HashMap<>();
        osDistribution.put(os, 1);

        Map<String, Integer> deviceTypeDistribution = new HashMap<>();
        deviceTypeDistribution.put(deviceType, 1);

        return BrowserStatistics.BrowserInfo.builder()
                .count(1)
                .percentage(0.0) // 需要在外部计算
                .version(version)
                .osDistribution(osDistribution)
                .deviceTypeDistribution(deviceTypeDistribution)
                .marketRank(getBrowserMarketRank(browser))
                .isModern(isModernBrowser(browser))
                .build();
    }

    /**
     * 检测浏览器类型
     *
     * @param userAgent User-Agent字符串
     * @return 浏览器类型
     */
    public static String detectBrowser(String userAgent) {
        if (userAgent.contains("Edg"))
            return "Edge";
        if (userAgent.contains("Chrome"))
            return "Chrome";
        if (userAgent.contains("Firefox"))
            return "Firefox";
        if (userAgent.contains("Safari") && !userAgent.contains("Chrome"))
            return "Safari";
        if (userAgent.contains("Opera") || userAgent.contains("OPR"))
            return "Opera";
        if (userAgent.contains("MSIE") || userAgent.contains("Trident"))
            return "IE";
        return "其他";
    }

    /**
     * 提取浏览器版本
     *
     * @param userAgent User-Agent字符串
     * @param browser   浏览器类型
     * @return 浏览器版本
     */
    public static String extractBrowserVersion(String userAgent, String browser) {
        Pattern pattern = BROWSER_VERSION_PATTERNS.get(browser);
        if (pattern != null) {
            Matcher matcher = pattern.matcher(userAgent);
            if (matcher.find()) {
                return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            }
        }
        return "unknown";
    }

    /**
     * 检测操作系统
     *
     * @param userAgent User-Agent字符串
     * @return 操作系统
     */
    public static String detectOperatingSystem(String userAgent) {
        for (Map.Entry<String, Pattern> entry : OS_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(userAgent).find()) {
                return entry.getKey();
            }
        }
        return "Unknown";
    }

    /**
     * 检测设备类型
     *
     * @param userAgent User-Agent字符串
     * @return 设备类型
     */
    public static String detectDeviceType(String userAgent) {
        for (Map.Entry<String, List<String>> entry : DEVICE_TYPE_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (userAgent.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return "Unknown";
    }

    /**
     * 获取浏览器市场排名
     *
     * @param browser 浏览器类型
     * @return 市场排名
     */
    public static Integer getBrowserMarketRank(String browser) {
        return switch (browser) {
            case "Chrome" -> 1;
            case "Safari" -> 2;
            case "Edge" -> 3;
            case "Firefox" -> 4;
            case "Opera" -> 5;
            case "IE" -> 6;
            default -> 7;
        };
    }

    /**
     * 判断是否为现代浏览器
     *
     * @param browser 浏览器类型
     * @return 是否为现代浏览器
     */
    public static Boolean isModernBrowser(String browser) {
        return !browser.equals("IE");
    }

    /**
     * 计算访问强度等级
     *
     * @param count 访问次数
     * @return 强度等级
     */
    public static String calculateIntensity(Long count) {
        if (count < 10)
            return "low";
        if (count < 50)
            return "medium";
        if (count < 100)
            return "high";
        return "very_high";
    }

    /**
     * 计算风险等级
     *
     * @param count   访问次数
     * @param isLocal 是否为本地IP
     * @return 风险等级
     */
    public static String calculateRiskLevel(Integer count, Boolean isLocal) {
        if (isLocal)
            return "low";
        if (count > 1000)
            return "high";
        if (count > 100)
            return "medium";
        return "low";
    }

    /**
     * 获取HTTP方法描述
     *
     * @param method HTTP方法
     * @return 方法描述
     */
    public static String getMethodDescription(String method) {
        return switch (method.toUpperCase()) {
            case "GET" -> "获取资源";
            case "POST" -> "创建资源";
            case "PUT" -> "更新资源";
            case "DELETE" -> "删除资源";
            case "HEAD" -> "获取头部信息";
            case "OPTIONS" -> "获取支持的方法";
            case "PATCH" -> "部分更新资源";
            case "TRACE" -> "路径追踪";
            case "CONNECT" -> "建立隧道连接";
            default -> "其他方法";
        };
    }

    /**
     * 判断是否为安全HTTP方法
     *
     * @param method HTTP方法
     * @return 是否为安全方法
     */
    public static Boolean isSafeMethod(String method) {
        return Arrays.asList("GET", "HEAD", "OPTIONS", "TRACE").contains(method.toUpperCase());
    }

    /**
     * 判断是否为幂等HTTP方法
     *
     * @param method HTTP方法
     * @return 是否为幂等方法
     */
    public static Boolean isIdempotentMethod(String method) {
        return Arrays.asList("GET", "HEAD", "PUT", "DELETE", "OPTIONS", "TRACE").contains(method.toUpperCase());
    }

    /**
     * 获取星期几的中文名称
     *
     * @param weekday 星期几（1-7，1为周日）
     * @return 中文名称
     */
    public static String getDayName(Integer weekday) {
        String[] dayNames = { "", "周日", "周一", "周二", "周三", "周四", "周五", "周六" };
        return weekday >= 1 && weekday <= 7 ? dayNames[weekday] : "未知";
    }

    /**
     * 判断是否为工作日
     *
     * @param weekday 星期几（1-7，1为周日）
     * @return 是否为工作日
     */
    public static Boolean isWorkday(Integer weekday) {
        return weekday >= 2 && weekday <= 6; // 周一到周五
    }

    /**
     * 判断是否为周末
     *
     * @param weekday 星期几（1-7，1为周日）
     * @return 是否为周末
     */
    public static Boolean isWeekend(Integer weekday) {
        return weekday == 1 || weekday == 7; // 周日和周六
    }

    /**
     * 计算数据质量评分
     *
     * @param parsedLines 成功解析的行数
     * @param totalLines  总行数
     * @return 质量评分（0-100）
     */
    public static Integer calculateQualityScore(long parsedLines, long totalLines) {
        if (totalLines == 0)
            return 0;
        double rate = (double) parsedLines / totalLines;
        return (int) (rate * 100);
    }

    /**
     * 安全地解析Double值
     *
     * @param value 字符串值
     * @return Double值，解析失败返回null
     */
    public static Double parseDouble(String value) {
        try {
            return value != null && !value.trim().isEmpty() ? Double.parseDouble(value.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 安全地解析Integer值
     *
     * @param value 字符串值
     * @return Integer值，解析失败返回null
     */
    public static Integer parseInt(String value) {
        try {
            return value != null && !value.trim().isEmpty() ? Integer.parseInt(value.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 格式化当前时间为指定格式
     *
     * @param formatter 日期格式化器
     * @return 格式化后的时间字符串
     */
    public static String formatCurrentTime(DateTimeFormatter formatter) {
        return LocalDateTime.now().format(formatter);
    }

    /**
     * 获取日期格式化器
     *
     * @return 日期格式化器
     */
    public static DateTimeFormatter getDateFormatter() {
        return DATE_FORMATTER;
    }

    /**
     * 获取小时格式化器
     *
     * @return 小时格式化器
     */
    public static DateTimeFormatter getHourFormatter() {
        return HOUR_FORMATTER;
    }

    /**
     * 获取分钟格式化器
     *
     * @return 分钟格式化器
     */
    public static DateTimeFormatter getMinuteFormatter() {
        return MINUTE_FORMATTER;
    }

    /**
     * 创建默认的浏览器信息
     *
     * @return 默认浏览器信息
     */
    private static BrowserStatistics.BrowserInfo createDefaultBrowserInfo() {
        return BrowserStatistics.BrowserInfo.builder()
                .count(1)
                .percentage(0.0)
                .version("unknown")
                .osDistribution(Map.of("Unknown", 1))
                .deviceTypeDistribution(Map.of("Unknown", 1))
                .marketRank(7)
                .isModern(false)
                .build();
    }
}