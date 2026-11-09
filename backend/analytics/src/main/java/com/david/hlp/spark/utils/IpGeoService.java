package com.david.hlp.spark.utils;

import com.maxmind.db.Reader;
import jakarta.annotation.PreDestroy;
import org.apache.hc.core5.net.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Optional, read-only DB-IP Lite City enrichment. Never performs network I/O. */
@Service
public class IpGeoService implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(IpGeoService.class);
    private Reader reader;
    private String status = "unconfigured";

    public IpGeoService(@Value("${geolocation.mmdb-path:}") String path) {
        if (path == null || path.isBlank()) {
            log.warn("Offline geolocation unconfigured; locations will be unknown");
            return;
        }
        try {
            reader = new Reader(new File(path), Reader.FileMode.MEMORY);
            status = "ready";
            log.info("Offline geolocation ready (DB-IP Lite City)");
        } catch (IOException | RuntimeException e) {
            status = "unavailable";
            log.warn("Offline geolocation database unavailable ({}); locations will be unknown", e.getClass().getSimpleName());
        }
    }

    public synchronized String getStatus() { return status; }

    public synchronized Map<String, String> getIpGeoInfo(String ip) {
        Map<String, String> result = new HashMap<>();
        result.put("country_name", "");
        result.put("city", "");
        InetAddress address = literal(ip);
        if (reader == null || address == null || !isPublic(address)) return result;
        try {
            Map<?, ?> record = reader.get(address, Map.class);
            if (record == null) return result;
            result.put("country_name", name(record.get("country")));
            result.put("city", name(record.get("city")));
            if (record.get("subdivisions") instanceof List<?> regions && !regions.isEmpty()) {
                result.put("region", name(regions.get(0)));
            }
            if (record.get("location") instanceof Map<?, ?> location) {
                Object lat = location.get("latitude"), lon = location.get("longitude");
                if (lat instanceof Number a && lon instanceof Number b
                        && Double.isFinite(a.doubleValue()) && Math.abs(a.doubleValue()) <= 90
                        && Double.isFinite(b.doubleValue()) && Math.abs(b.doubleValue()) <= 180) {
                    result.put("latitude", a.toString());
                    result.put("longitude", b.toString());
                }
            }
        } catch (IOException | RuntimeException e) {
            status = "lookup-failed";
            log.warn("Offline geolocation lookup failed ({}); locations will be unknown", e.getClass().getSimpleName());
            try { close(); } catch (IOException ignored) { /* reader already disabled */ }
            result.clear();
            result.put("country_name", "");
            result.put("city", "");
        }
        return result;
    }

    private static String name(Object value) {
        if (value instanceof Map<?, ?> item && item.get("names") instanceof Map<?, ?> names
                && names.get("en") instanceof String text) return text;
        return "";
    }

    static InetAddress literal(String ip) {
        if (ip == null || ip.contains("%") || !(InetAddressUtils.isIPv4Address(ip)
                || InetAddressUtils.isIPv6Address(ip))) return null;
        try { return InetAddress.getByName(ip); } // validated literal: no DNS
        catch (UnknownHostException e) { return null; }
    }

    public boolean isLocalIp(String ip) {
        InetAddress address = literal(ip);
        return address != null && !isPublic(address);
    }

    private static boolean isPublic(InetAddress address) {
        byte[] bytes = address.getAddress();
        int a = bytes[0] & 255, b = bytes[1] & 255;
        if (bytes.length == 16) {
            // Global unicast only; exclude special-purpose and documentation prefixes.
            return (a & 224) == 32 && !(a == 32 && b == 1 && (bytes[2] & 254) == 0)
                    && !(a == 32 && b == 1 && (bytes[2] & 255) == 13 && (bytes[3] & 255) == 184)
                    && !(a == 63 && b == 255 && (bytes[2] & 240) == 0);
        }
        int c = bytes[2] & 255;
        return a != 0 && a != 10 && a != 127 && a < 224
                && !(a == 100 && b >= 64 && b <= 127)
                && !(a == 169 && b == 254) && !(a == 172 && b >= 16 && b <= 31)
                && !(a == 192 && (b == 168 || (b == 0 && (c == 0 || c == 2)) || (b == 88 && c == 99)))
                && !(a == 198 && (b == 18 || b == 19 || (b == 51 && c == 100)))
                && !(a == 203 && b == 0 && c == 113);
    }

    @PreDestroy
    public synchronized void close() throws IOException {
        Reader previous = reader;
        reader = null;
        if (!"lookup-failed".equals(status)) status = "closed";
        if (previous != null) previous.close();
    }
}
