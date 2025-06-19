package com.david.hlp.web.spark.boss.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.david.hlp.web.spark.boss.service.BossBasicServiceImp;

import lombok.RequiredArgsConstructor;
import java.util.List;

@RestController
@RequestMapping("/api/boss/basic")
@RequiredArgsConstructor
public class BossBasicController {

        private final BossBasicServiceImp listBasicSetService;

        @GetMapping("/city-name-list")
        public List<String> getCityNameList() {
                return listBasicSetService.getCityNameByList();
        }

        @GetMapping("/position-name-list")
        public List<String> getPositionNameList() {
                return listBasicSetService.getPositionNameByList();
        }
}