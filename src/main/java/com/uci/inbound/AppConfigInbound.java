package com.uci.inbound;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.uci.dao.service.HealthService;

@Configuration
public class AppConfigInbound {
    @Bean 
    public HealthService healthService() {
    	return new HealthService();
    }
}
