package com.uci.inbound;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import com.uci.dao.service.HealthService;

@Configuration
public class AppConfigInbound {
	@Value("${spring.kafka.bootstrap-servers}")
    	private String BOOTSTRAP_SERVERS;
	
	@Value("${spring.redis.database}")
	private String redisDb;
	
	@Value("${spring.redis.host}")
	private String redisHost;
	
	@Value("${spring.redis.port}")
	private String redisPort;
	
	@Bean
	public HealthService healthService() {
		return new HealthService();
	}

	@Bean
    Map<String, Object> kafkaProducerConfiguration() {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        configuration.put(org.springframework.kafka.support.serializer.JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        configuration.put(ProducerConfig.CLIENT_ID_CONFIG, "sample-producer");
        configuration.put(ProducerConfig.ACKS_CONFIG, "all");
        configuration.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonSerializer.class);
        configuration.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonSerializer.class);
        
        return configuration;
    }
    
    @Bean
    ProducerFactory<String, String> producerFactory(){
    	ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(kafkaProducerConfiguration());
    	return producerFactory;
    }
    
    @Bean
    KafkaTemplate<String, String> kafkaTemplate() {
    	KafkaTemplate<String, String> kafkaTemplate = new KafkaTemplate<>(producerFactory());
    	return (KafkaTemplate<String, String>) kafkaTemplate;
    }

	@Bean
	JedisConnectionFactory jedisConnectionFactory() {
		JedisConnectionFactory jedisConFactory
	      = new JedisConnectionFactory();
	    jedisConFactory.setHostName(redisHost);
	    Integer port = Integer.parseInt(redisPort);
	    jedisConFactory.setPort(port);
	    Integer dbIndex = Integer.parseInt(redisDb);
	    jedisConFactory.setDatabase(dbIndex);
//		jedisConFactory.getPoolConfig().setMaxIdle(30);
//		jedisConFactory.getPoolConfig().setMinIdle(10);
	    return jedisConFactory;
	}

	@Bean
	public RedisTemplate<String, Object> redisTemplate() {
	    RedisTemplate<String, Object> template = new RedisTemplate<>();
	    template.setConnectionFactory(jedisConnectionFactory());
	    template.setKeySerializer(new StringRedisSerializer());
	    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
//	    template.setEnableTransactionSupport(true);
	    return template;
	}
}
