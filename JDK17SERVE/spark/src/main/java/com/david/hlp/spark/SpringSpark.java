package com.david.hlp.spark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringSpark {
    public static void main(String[] args) {
        SpringApplication.run(SpringSpark.class, args);
    }
}