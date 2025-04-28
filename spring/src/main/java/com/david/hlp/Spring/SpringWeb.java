package com.david.hlp.Spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringWeb {

	public static void main(String[] args) {
		SpringApplication.run(SpringWeb.class, args);
	}

}
