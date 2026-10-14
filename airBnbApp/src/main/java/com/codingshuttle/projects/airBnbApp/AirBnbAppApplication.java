package com.codingshuttle.projects.airBnbApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Authentication is JWT-only (see JwtAuthFilter); skip Boot's default in-memory user and its logged password
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class AirBnbAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(AirBnbAppApplication.class, args);
	}

}
