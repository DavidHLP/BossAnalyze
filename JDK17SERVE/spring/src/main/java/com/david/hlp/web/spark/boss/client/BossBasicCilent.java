package com.david.hlp.web.spark.boss.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "SpringSpark", contextId = "bossBasicClient")
public interface BossBasicCilent {
    @GetMapping("/api/v1/boss/basic/city-name-list")
    public List<String> getCityNameList();

    @GetMapping("/api/v1/boss/basic/position-name-list")
    public List<String> getPositionNameList();
}
