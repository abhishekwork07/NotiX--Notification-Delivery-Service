package com.abhishek.notix.retry_scheduler_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RetrySchedulerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RetrySchedulerServiceApplication.class, args);
	}

}
