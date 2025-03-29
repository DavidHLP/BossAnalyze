package com.david.hlp.Spring.boss.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.david.hlp.Spring.boss.service.RedisReadServiceImp;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/boss")
@RequiredArgsConstructor
public class RedisReadController {
    private final RedisReadServiceImp redisReadServiceImp;

    @GetMapping("/jobs")
    public List<Map<String, Object>> listAllJobs() {
        return redisReadServiceImp.listAllJobData();
    }

    @GetMapping("/jobs/position/{position}")
    public List<Map<String, Object>> getJobsByPosition(@PathVariable String position) {
        return redisReadServiceImp.listJobDataByPosition(position);
    }

    @GetMapping("/jobs/position")
    public List<Map<String, Object>> getJobsByPositionParam(@RequestParam String position) {
        return redisReadServiceImp.listJobDataByPosition(position);
    }

    @GetMapping("/jobs/position/{position}/time/{timePeriod}")
    public List<Map<String, Object>> getJobsByPositionAndTime(@PathVariable String position, @PathVariable String timePeriod) {
        return redisReadServiceImp.listJobDataByPositionAndTime(position, timePeriod);
    }

    @GetMapping("/jobs/position/time")
    public List<Map<String, Object>> getJobsByPositionAndTimeParam(@RequestParam String position, @RequestParam String timePeriod) {
        return redisReadServiceImp.listJobDataByPositionAndTime(position, timePeriod);
    }

    @GetMapping("/positions")
    public List<String> listAllPositions() {
        return redisReadServiceImp.listAllPositions();
    }

    @GetMapping("/timePeriods")
    public List<String> listAllTimePeriods() {
        return redisReadServiceImp.listAllTimePeriods();
    }

    @GetMapping("/cities")
    public List<String> listAllCities() {
        return redisReadServiceImp.listAllCities();
    }
}