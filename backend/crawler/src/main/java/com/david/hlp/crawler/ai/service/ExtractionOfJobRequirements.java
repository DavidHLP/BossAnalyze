package com.david.hlp.crawler.ai.service;

import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;

@Service
@ConditionalOnProperty(name="crawler.ai.scheduling.enabled",havingValue="true",matchIfMissing=false)
public class ExtractionOfJobRequirements {
    private final JobAnalysisStore store;
    private final long start,end;
    public ExtractionOfJobRequirements(JobAnalysisStore store,@Value("${crawler.ai.range-start:0}")long start,
            @Value("${crawler.ai.range-end:0}")long end){this.store=store;this.start=start;this.end=end;}
    @Scheduled(fixedDelayString="${crawler.ai.interval-ms:300000}")
    public void analyze(){store.process(start,end);}
}
