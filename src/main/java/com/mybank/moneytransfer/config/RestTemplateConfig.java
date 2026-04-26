package com.mybank.moneytransfer.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

	/** timeout In Milliseconds **/
	@Value("${http.connection.timeoutInMS}")
	private Integer timeoutInMS;

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder
				.connectTimeout(Duration.ofMillis(timeoutInMS))
				.readTimeout(Duration.ofMillis(timeoutInMS))
				.build();
	}
}
