package com.example.distribution_backernd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DistributionBackerndApplication {

	public static void main(String[] args) {
		SpringApplication.run(DistributionBackerndApplication.class, args);
	}

}
