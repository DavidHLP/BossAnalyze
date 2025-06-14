package com.david.hlp.web.common.enums;

import java.util.concurrent.TimeUnit;

public enum RedisKeyCommon {

    REGISTER_CODE_KEY("register:code:", 1, TimeUnit.MINUTES),
    REGISTER_USER_KEY("register:user:", 10, TimeUnit.SECONDS),
    TOKEN_ACCESS_KEY("token:access:", 2, TimeUnit.HOURS),
    TOKEN_REFRESH_KEY("token:refresh:", 7, TimeUnit.DAYS),
    RESUME_LIST_KEY("resume:list:", 10, TimeUnit.MINUTES),
    SALARY_ANALYSIS_KEY("salary:analysis:", 10, TimeUnit.MINUTES),
    SALARY_BASES_KEY("salary:bases:", 10, TimeUnit.MINUTES);

    private final String key;
    private final long timeout;
    private final TimeUnit timeUnit;

    RedisKeyCommon(String key, long timeout, TimeUnit timeUnit) {
        this.key = key;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    public String getKey() {
        return key;
    }

    public long getTimeout() {
        return timeout;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public String toString() {
        return key;
    }
}
