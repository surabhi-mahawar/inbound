package com.uci.inbound.health;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {
	@Value("${spring.kafka.bootstrap-servers}")
	String bootstrapServer;

	@Bean
	public AdminClient kafkaAdminClient() {
		Properties properties = new Properties();
		properties.put("bootstrap.servers", "165.232.182.146:9094");
//		properties.put("bootstrap.servers", bootstrapServer);
		properties.put("connections.max.idle.ms", 10);
		properties.put("request.timeout.ms", 5000);
		return AdminClient.create(properties);
	}

	@Bean
	public HealthIndicator kafkaHealthIndicator() {
		final DescribeClusterOptions describeClusterOptions = new DescribeClusterOptions().timeoutMs(1000);
		final AdminClient adminClient = kafkaAdminClient();
		return () -> {
			final DescribeClusterResult describeCluster = adminClient.describeCluster(describeClusterOptions);
			try {
				final String clusterId = describeCluster.clusterId().get();
				final int nodeCount = describeCluster.nodes().get().size();
//				System.out.println("Cluster ID: "+clusterId);
//				System.out.println("Cluster count: "+nodeCount);
				ListTopicsOptions options = new ListTopicsOptions();
			    options.listInternal(true); // includes internal topics such as __consumer_offsets
			    ListTopicsResult topics = adminClient.listTopics(options);
			    Set<String> currentTopicList = topics.names().get();
			    final int currentTopicCount = currentTopicList.size();
			    System.out.println("Current Topics List: "+currentTopicList);
			    System.out.println("Current Topics Count: "+currentTopicCount);
			    
				return Health.up()
						.withDetail("clusterId", clusterId)
						.withDetail("nodeCount", nodeCount)
						.withDetail("topicsExists", currentTopicCount > 0 ? true : false)
						.build();
			} catch (InterruptedException | ExecutionException e) {
				return Health.down()
						.withException(e)
						.build();
			}
		};

	}

}
