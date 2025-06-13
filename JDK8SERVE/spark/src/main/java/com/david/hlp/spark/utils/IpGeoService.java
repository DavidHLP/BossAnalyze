package com.david.hlp.spark.utils;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.hc.core5.http.ParseException;

/**
 * IP地理信息服务
 */
@Service
public class IpGeoService {
    private static final String API_KEY = "266a9419b23942ebb641cb20985db574";
    private static final String API_URL = "https://api.ipgeolocation.io/ipgeo";

    // IP地址相关的正则表达式
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(127\\.0\\.0\\.1|10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|" +
                    "172\\.(1[6-9]|2[0-9]|3[0-1])\\.\\d{1,3}\\.\\d{1,3}|" +
                    "192\\.168\\.\\d{1,3}\\.\\d{1,3}|" +
                    "169\\.254\\.\\d{1,3}\\.\\d{1,3}|" +
                    "0\\.0\\.0\\.0|255\\.255\\.255\\.255)$");

    /**
     * 获取IP地址的地理信息
     * 
     * @param ip IP地址
     * @return 包含国家名称和城市的地图，如果查询失败则返回空值
     */
    public Map<String, String> getIpGeoInfo(String ip) {
        if (ip == null || ip.isEmpty()) {
            return createEmptyGeoInfo();
        }

        if (isLocalIp(ip)) {
            Map<String, String> result = new HashMap<>();
            result.put("country_name", "本地网络");
            result.put("city", getLocalIpType(ip));
            return result;
        }

        String url = String.format("%s?ip=%s&apiKey=%s", API_URL, ip, API_KEY);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            request.setHeader("Accept", "application/json");

            try (ClassicHttpResponse response = httpClient.executeOpen(null, request, null)) {
                if (response.getCode() != 200) {
                    return createEmptyGeoInfo();
                }

                String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
                JSONObject json = new JSONObject(jsonResponse);

                Map<String, String> result = new HashMap<>();
                result.put("country_name", json.optString("country_name", ""));
                result.put("city", json.optString("city", ""));
                return result;
            } catch (ParseException e) {
                return createEmptyGeoInfo();
            }
        } catch (IOException e) {
            return createEmptyGeoInfo();
        }
    }

    /**
     * 创建空的IP地理信息
     */
    private Map<String, String> createEmptyGeoInfo() {
        Map<String, String> result = new HashMap<>();
        result.put("country_name", "");
        result.put("city", "");
        return result;
    }

    /**
     * 判断IP是否为本地地址
     * 
     * @param ip 要检查的IP地址
     * @return 如果是本地地址返回true，否则返回false
     */
    public boolean isLocalIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // IPv6回环地址
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return true;
        }

        // IPv4地址检查
        return IPV4_PATTERN.matcher(ip).matches();
    }

    /**
     * 获取本地IP的类型描述
     */
    private String getLocalIpType(String ip) {
        if (ip.startsWith("127.")) {
            return "本地回环地址";
        } else if (ip.startsWith("10.")) {
            return "A类私有地址";
        } else if (ip.startsWith("172.16.") || ip.startsWith("172.17.") ||
                ip.startsWith("172.18.") || ip.startsWith("172.19.") ||
                ip.startsWith("172.20.") || ip.startsWith("172.21.") ||
                ip.startsWith("172.22.") || ip.startsWith("172.23.") ||
                ip.startsWith("172.24.") || ip.startsWith("172.25.") ||
                ip.startsWith("172.26.") || ip.startsWith("172.27.") ||
                ip.startsWith("172.28.") || ip.startsWith("172.29.") ||
                ip.startsWith("172.30.") || ip.startsWith("172.31.")) {
            return "B类私有地址";
        } else if (ip.startsWith("192.168.")) {
            return "C类私有地址";
        } else if (ip.startsWith("169.254.")) {
            return "链路本地地址";
        } else if (ip.equals("0.0.0.0")) {
            return "本机地址";
        } else if (ip.equals("255.255.255.255")) {
            return "广播地址";
        } else {
            return "本地网络";
        }
    }
}
