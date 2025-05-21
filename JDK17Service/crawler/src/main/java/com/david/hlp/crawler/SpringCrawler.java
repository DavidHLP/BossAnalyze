package com.david.hlp.crawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringCrawler {

	public static void main(String[] args) {
		SpringApplication.run(SpringCrawler.class, args);
	}

}
