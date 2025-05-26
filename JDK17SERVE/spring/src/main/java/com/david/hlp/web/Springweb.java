package com.david.hlp.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Springweb {

	public static void main(String[] args) {
		SpringApplication.run(Springweb.class, args);
	}

}
