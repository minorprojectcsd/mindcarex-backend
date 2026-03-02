package com.mindcarex.mindcarex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync  // Enable async email sending
@EnableScheduling  // ⭐ Enable scheduled tasks
public class MindcarexApplication {
	public static void main(String[] args) {
		SpringApplication.run(MindcarexApplication.class, args);
	}
}