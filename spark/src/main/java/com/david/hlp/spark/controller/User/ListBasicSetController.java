package com.david.hlp.spark.controller.User;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

import com.david.hlp.spark.service.User.ListBasicSet;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/boss/basic")
@RequiredArgsConstructor
public class ListBasicSetController {
    private final ListBasicSet listBasicSet;

    @GetMapping("/city-name-list")
    public List<String> getCityNameList() {
        return listBasicSet.getCityNameList();
    }

    @GetMapping("/position-name-list")
    public List<String> getPositionNameList(@RequestParam(defaultValue = "all") String cityName) {
        return listBasicSet.getPositionNameList(cityName);
    }
}
