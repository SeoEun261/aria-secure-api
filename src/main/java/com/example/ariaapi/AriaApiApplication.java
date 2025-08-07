package com.example.ariaapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;

@SpringBootApplication

public class AriaApiApplication {
	@Value("${server.port}")
	private String serverPortCheck;

	public static void main(String[] args) {
		SpringApplication.run(AriaApiApplication.class, args);
	}

	@PostConstruct
	public void checkPropertiesLoading() {
		System.out.println("DEBUG: server.port from application.properties = " + serverPortCheck);

	}
}
