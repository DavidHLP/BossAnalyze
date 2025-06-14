package com.david.hlp.web.boss.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;

import com.david.hlp.web.boss.client.BossBasicCilent;
import com.david.hlp.web.common.enums.RedisKeyCommon;
import com.david.hlp.web.common.enums.RedisLockKeyCommon;
import com.david.hlp.web.common.util.RedisCache;

@Service
@RequiredArgsConstructor
public class BossBasicServiceImp {
        private final BossBasicCilent bossBasicCilent;
        private final RedisCache redisCache;

        public List<String> getCityNameByList() {
                return redisCache.getWithMutex(
                                RedisKeyCommon.SALARY_BASES_KEY.getKey() + "city",
                                RedisKeyCommon.SALARY_BASES_KEY.getTimeout(),
                                RedisKeyCommon.SALARY_BASES_KEY.getTimeUnit(),
                                RedisLockKeyCommon.SALARY_BASES_LOCK_KEY.getKey() + "city",
                                RedisLockKeyCommon.SALARY_BASES_LOCK_KEY.getWaitTime(),
                                RedisLockKeyCommon.SALARY_BASES_LOCK_KEY.getLeaseTime(),
                                RedisLockKeyCommon.SALARY_BASES_LOCK_KEY.getTimeUnit(),
                                () -> bossBasicCilent.getCityNameList());
        }

        public List<String> getPositionNameByList() {
                return redisCache.getWithMutex(
                                RedisKeyCommon.SALARY_BASES_KEY.getKey() + "position",
                                RedisKeyCommon.SALARY_BASES_KEY.getTimeout(),
                                RedisKeyCommon.SALARY_BASES_KEY.getTimeUnit(),
                                RedisLockKeyCommon.SALARY_BASES_LOCK_KEY.getKey() + "position",
                                RedisLockKeyCommon.SALARY_BASES_LOCK_KEY.getWaitTime(),
                                RedisLockKeyCommon.SALARY_BASES_LOCK_KEY.getLeaseTime(),
                                RedisLockKeyCommon.SALARY_BASES_LOCK_KEY.getTimeUnit(),
                                () -> bossBasicCilent.getPositionNameList());
        }
}
