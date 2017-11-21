package com.emarbox.consumerstream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class ConsumerStreamApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(ConsumerStreamApplication.class, args);
	}
	
}
