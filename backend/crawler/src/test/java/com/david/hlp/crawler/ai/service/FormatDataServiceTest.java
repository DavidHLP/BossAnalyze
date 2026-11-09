package com.david.hlp.crawler.ai.service;

import com.david.hlp.crawler.ai.entity.MiniJobDetail;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormatDataServiceTest {
    private final FormatDataService service = new FormatDataService(new ObjectMapper());

    @Test
    void preservesMissingAddressAndStoredSourceUrl() throws Exception {
        var json = new ObjectMapper();
        for (String detail : new String[]{"{}", "{\"basicInfo\":{}}",
                "{\"basicInfo\":{\"address\":null}}"}) {
            var job = MiniJobDetail.builder().id(1L).detailData(detail)
                    .url("https://www.zhipin.com/job_detail/synthetic.html").build();
            var result = service.convertToJobAnalysisData(job);
            assertNotNull(result);
            assertNull(result.getAddress());
            var response = json.valueToTree(result);
            assertTrue(response.path("address").isNull());
            assertEquals(job.getUrl(), response.path("jobUrl").asText());
        }
    }

    @Test
    void retainsKnownAddressAndAbsentSourceUrl() {
        var job = MiniJobDetail.builder().id(2L)
                .detailData("{\"basicInfo\":{\"address\":\"杭州·西湖区\"}}")
                .build();
        var result = service.convertToJobAnalysisData(job);
        assertNotNull(result);
        assertEquals("杭州·西湖区", result.getAddress());
        assertNull(result.getJobUrl());
    }
}
