package com.david.hlp.spark.utils;

import com.maxmind.db.Reader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Import;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IpGeoServiceTest {
    @Import(IpGeoService.class) static class Config { }
    @TempDir Path temporary;

    @Test void springStartsWithoutKeyOrDatabaseAndBadFilesDegrade() throws Exception {
        new ApplicationContextRunner().withUserConfiguration(Config.class).run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals("unconfigured", context.getBean(IpGeoService.class).getStatus());
        });
        for (String path : new String[]{temporary.resolve("missing").toString(), Files.writeString(temporary.resolve("bad"), "not mmdb").toString()}) {
            new ApplicationContextRunner().withUserConfiguration(Config.class).withPropertyValues("geolocation.mmdb-path=" + path).run(context -> {
                assertNull(context.getStartupFailure());
                var service = context.getBean(IpGeoService.class);
                assertEquals("unavailable", service.getStatus());
                assertEquals("", service.getIpGeoInfo("8.8.8.8").get("country_name"));
                assertFalse(service.getIpGeoInfo("8.8.8.8").containsKey("latitude"));
            });
        }
    }

    @Test void strictLiteralsAndNonPublicAddressesNeverReachReader() throws Exception {
        try (var construction = mockConstruction(Reader.class)) {
            var service = new IpGeoService("synthetic");
            for (String ip : new String[]{null, "", "localhost", "example.com", "127.1", "1", "01.2.3.4", "999.1.1.1", "8.8.8.8:80", "::1%lo", "[::1]", "::1", "127.2.3.4", "10.1.2.3", "172.31.2.3", "192.168.0.1", "100.64.0.1", "169.254.0.1", "224.0.0.1", "240.0.0.1", "192.0.2.1", "198.51.100.1", "203.0.113.1", "fc00::1", "fe80::1", "ff02::1", "2001:db8::1", "::ffff:192.168.1.1"}) {
                assertEquals("", service.getIpGeoInfo(ip).get("country_name"), String.valueOf(ip));
            }
            verifyNoInteractions(construction.constructed().get(0));
            service.close();
        }
    }

    @Test void decodesV4V6WithoutInventingCoordinatesAndClosesSafely() throws Exception {
        Map<String, Object> record = Map.of("country", Map.of("names", Map.of("en", "Testland")), "location", Map.of("latitude", 12.0, "longitude", 34.0));
        try (var construction = mockConstruction(Reader.class, (reader, context) -> when(reader.get(any(), eq(Map.class))).thenReturn(record))) {
            var service = new IpGeoService("synthetic");
            var pool = Executors.newFixedThreadPool(4);
            try {
                var calls = IntStream.range(0, 40).mapToObj(i -> (Callable<Map<String, String>>) () -> service.getIpGeoInfo(i % 2 == 0 ? "8.8.8.8" : "2001:4860:4860::8888")).toList();
                for (var result : pool.invokeAll(calls)) assertEquals("12.0", result.get().get("latitude"));
            } finally { pool.shutdownNow(); }
            var reader = construction.constructed().get(0);
            when(reader.get(any(), eq(Map.class))).thenReturn(Map.of("country", Map.of("names", Map.of("en", "Testland"))));
            assertFalse(service.getIpGeoInfo("8.8.8.8").containsKey("latitude"));
            when(reader.get(any(), eq(Map.class))).thenReturn(Map.of("location", Map.of("latitude", Double.NaN, "longitude", 12.0)));
            assertFalse(service.getIpGeoInfo("8.8.8.8").containsKey("latitude"));
            when(reader.get(any(), eq(Map.class))).thenThrow(new java.io.IOException("synthetic failure"));
            assertEquals("", service.getIpGeoInfo("8.8.8.8").get("country_name"));
            assertEquals("lookup-failed", service.getStatus());
            service.close(); service.close();
            verify(reader, times(1)).close();
            assertEquals("", service.getIpGeoInfo("8.8.8.8").get("country_name"));
        }
    }

    @Test void officialDatabaseWhenExplicitlyProvided() throws Exception {
        String path = System.getProperty("geo.validation.mmdb");
        org.junit.jupiter.api.Assumptions.assumeTrue(path != null);
        try (var service = new IpGeoService(path)) {
            assertEquals("ready", service.getStatus());
            for (String ip : new String[]{"8.8.8.8", "2001:4860:4860::8888"}) {
                assertFalse(service.getIpGeoInfo(ip).get("country_name").isBlank());
                assertNotNull(service.getIpGeoInfo(ip).get("latitude"));
            }
        }
    }
}
