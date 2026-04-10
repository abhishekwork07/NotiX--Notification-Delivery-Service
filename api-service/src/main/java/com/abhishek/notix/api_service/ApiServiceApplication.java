package com.abhishek.notix.api_service;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

@SpringBootApplication
@EnableScheduling
@EnableEurekaServer
public class ApiServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiServiceApplication.class, args);
	}

	@Bean
	public ApplicationRunner kafkaCheck(KafkaTemplate<String, ?> kafkaTemplate) {
		return args -> {
			System.out.println("✅ KafkaTemplate is ready. Default topic config: " +
					kafkaTemplate.getDefaultTopic());
		};
	}

	@Bean
	public ApplicationRunner dbTest(DataSource dataSource) {
		return args -> {
			System.out.println("✅ Connected to PostgreSQL at: " + dataSource.getConnection().getMetaData().getURL());
		};
	}


}
