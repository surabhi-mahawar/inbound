package com.samagra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

//@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@SpringBootApplication
@EnableKafka
@EnableJpaRepositories("messagerosa.dao")
@EntityScan("messagerosa.dao")
@PropertySource("application-messagerosa.properties")
@PropertySource("application.properties")
@PropertySource("application-adapter.properties")
@ComponentScan(basePackages = {"com.samagra.*","com.*"})
public class Inbound {
	public static void main(String[] args) {
		SpringApplication.run(Inbound.class, args);
	}
}
