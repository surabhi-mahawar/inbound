package com.uci.inbound;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;

@Component
public class KakfaLogTopics {
	
	public void createTopic() {
		Properties properties = new Properties();
		properties.put("bootstrap.servers", "165.232.182.146:9094");
		properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		properties.put("group.id", "logs");

		KafkaConsumer kafkaConsumer = new KafkaConsumer(properties);
		List topics = new ArrayList();
		topics.add("inbound-logs");
		kafkaConsumer.subscribe(topics);
	}
}
