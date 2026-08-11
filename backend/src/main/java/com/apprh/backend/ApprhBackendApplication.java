package com.apprh.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ApprhBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApprhBackendApplication.class, args);
	}

}
