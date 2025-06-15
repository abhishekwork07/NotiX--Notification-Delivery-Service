package com.abhishek.notix.dispatcher_service;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

import javax.sql.DataSource;

@SpringBootApplication
public class DispatcherServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DispatcherServiceApplication.class, args);
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
