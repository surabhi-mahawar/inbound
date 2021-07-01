package com.uci.inbound;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
@EnableReactiveCassandraRepositories("com.uci.dao")
@PropertySource("application-messagerosa.properties")
@PropertySource("application.properties")
@PropertySource("application-adapter.properties")
@ComponentScan(basePackages = {"com.samagra.*", "com.uci.utils"})
public class Inbound {
	public static void main(String[] args) {
		SpringApplication.run(Inbound.class, args);
	}
}
