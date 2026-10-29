package com.david.hlp.commons.entity.logs;

public final class LogCacheKeys {
    public static final String ANALYSIS_RESULT_KEY = "log:analysis:complete";
    public static final String HEATMAP_KEY = "log:analysis:heatmap";
    public static final String IP_STATS_KEY = "log:analysis:ip-statistics";
    public static final String HTTP_METHODS_KEY = "log:analysis:http-methods";
    public static final String WEEKDAY_STATS_KEY = "log:analysis:weekday-patterns";
    public static final String BROWSER_STATS_KEY = "log:analysis:browser-usage";
    public static final long CACHE_EXPIRE_HOURS = 24;

    private LogCacheKeys() {
    }
}
