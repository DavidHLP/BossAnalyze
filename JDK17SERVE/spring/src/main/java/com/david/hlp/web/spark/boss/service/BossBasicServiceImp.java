package com.david.hlp.web.spark.boss.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;

import com.david.hlp.web.common.enums.RedisKeyEnum;
import com.david.hlp.web.spark.boss.client.BossBasicCilent;
import com.david.hlp.commons.utils.RedisCacheHelper;

@Service
@RequiredArgsConstructor
public class BossBasicServiceImp {
        private final BossBasicCilent bossBasicCilent;
        private final RedisCacheHelper redisCacheHelper;

        private static final String CACHE_KEY_CITY = "city";
        private static final String CACHE_KEY_POSITION = "position";

        public List<String> getCityNameByList() {
                String cacheKey = RedisKeyEnum.SALARY_BASES_KEY.getKey() + CACHE_KEY_CITY;

                return redisCacheHelper.getOrLoadList(
                                cacheKey,
                                String.class,
                                RedisKeyEnum.SALARY_BASES_KEY.getTimeout(),
                                () -> bossBasicCilent.getCityNameList());
        }

        public List<String> getPositionNameByList() {
                String cacheKey = RedisKeyEnum.SALARY_BASES_KEY.getKey() + CACHE_KEY_POSITION;

                return redisCacheHelper.getOrLoadList(
                                cacheKey,
                                String.class,
                                RedisKeyEnum.SALARY_BASES_KEY.getTimeout(),
                                () -> bossBasicCilent.getPositionNameList());
        }
}
