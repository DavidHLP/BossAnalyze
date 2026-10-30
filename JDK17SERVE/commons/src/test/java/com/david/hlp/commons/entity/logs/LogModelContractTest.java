package com.david.hlp.commons.entity.logs;

import java.util.Map;

import com.david.hlp.commons.utils.LogAnalysisUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogModelContractTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void browserAndMethodFlagsKeepFrontendJsonNames() throws Exception {
        var browser = LogAnalysisUtils.extractBrowserInfo("Mozilla/5.0 Chrome/120.0.0.0");
        var result = AccessLogAnalysisResult.builder()
                .browserStatistics(BrowserStatistics.builder().browserStats(Map.of("Chrome", browser)).build())
                .httpMethodStatistics(HttpMethodStatistics.builder().methodStats(Map.of("GET",
                        HttpMethodStatistics.MethodInfo.builder().count(1).isSafe(true).isIdempotent(true).build()))
                        .build())
                .build();

        JsonNode json = mapper.valueToTree(result);
        assertTrue(json.at("/browserStatistics/browserStats/Chrome/isModern").booleanValue());
        assertFalse(json.at("/browserStatistics/browserStats/Chrome").has("modern"));
        assertTrue(json.at("/httpMethodStatistics/methodStats/GET/isSafe").booleanValue());
        assertTrue(json.at("/httpMethodStatistics/methodStats/GET/isIdempotent").booleanValue());
        var restored = mapper.treeToValue(json, AccessLogAnalysisResult.class);
        assertEquals(browser, restored.getBrowserStatistics().getBrowserStats().get("Chrome"));
    }

    @Test
    void analysisTypeCodesMatchRoutesAndRejectUnknownValues() throws Exception {
        for (LogAnalysisType type : LogAnalysisType.values()) {
            assertSame(type, LogAnalysisType.fromCode(type.getCode()));
            assertEquals('"' + type.getCode() + '"', mapper.writeValueAsString(type));
        }
        assertNull(LogAnalysisType.fromCode("unsupported"));
        assertNull(LogAnalysisType.fromCode(null));
    }
}
