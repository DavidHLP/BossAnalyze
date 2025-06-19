package com.david.hlp.web.common.enums;

import java.util.concurrent.TimeUnit;

public enum RedisLockKeyEnum {
    SALARY_BASES_LOCK_KEY("salary:bases:lock:", 10, 30, TimeUnit.SECONDS),
    SALARY_ANALYSIS_LOCK_KEY("salary:analysis:lock:", 10, 30, TimeUnit.SECONDS),
    RESUME_LIST_LOCK_KEY("resume:list:lock:", 10, 30, TimeUnit.SECONDS),
    TOKEN_LOCK_KEY("token:lock:", 10, 30, TimeUnit.SECONDS),
    REGISTER_LOCK_KEY("register:lock:", 10, 30, TimeUnit.SECONDS);

    private final String key;
    private final long waitTime;
    private final long leaseTime;
    private final TimeUnit timeUnit;

    RedisLockKeyEnum(String key, long waitTime, long leaseTime, TimeUnit timeUnit) {
        this.key = key;
        this.waitTime = waitTime;
        this.leaseTime = leaseTime;
        this.timeUnit = timeUnit;
    }

    public String getKey() {
        return key;
    }

    public long getWaitTime() {
        return waitTime;
    }

    public long getLeaseTime() {
        return leaseTime;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
}
