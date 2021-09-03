package com.uci.inbound;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.uci.dao.service.HealthService;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class AppConfigInbound {
	@Bean
	public HealthService healthService() {
		return new HealthService();
	}

	@Bean
	public OpenAPI customOpenAPI(@Value("${application-title}") String appTitle,
			@Value("${application-version}") String appVersion) {
		return new OpenAPI().info(new Info().title(appTitle).version(appVersion)
				.description(appTitle + " Api Documentation").termsOfService("http://swagger.io/terms/")
				.license(new License().name("Apache 2.0").url("http://springdoc.org")));
	}
}
