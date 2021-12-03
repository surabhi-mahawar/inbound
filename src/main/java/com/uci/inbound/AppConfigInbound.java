package com.uci.inbound;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.uci.dao.service.HealthService;
import com.lightstep.opentelemetry.launcher.OpenTelemetryConfiguration;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

@Configuration
public class AppConfigInbound {
	@Bean
	public HealthService healthService() {
		return new HealthService();
	}
}
