package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class Zentrix1Application {

	public static void main(String[] args) {
		SpringApplication.run(Zentrix1Application.class, args);
		System.out.println("Hello folks");
	}
}
