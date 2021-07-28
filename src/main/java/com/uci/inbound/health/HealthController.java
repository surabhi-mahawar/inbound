package com.uci.inbound.health;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uci.utils.BotService;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.cassandra.CassandraHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class HealthController {
	
	@Value("${campaign.url}")
	String campaignUrl;
	
	@Autowired
	private KafkaConfig kafkaConfig;
	
	@Autowired
	private CassandraOperations cassandraOperations;
	
	@Autowired
	private BotService botService;
	
    @RequestMapping(value = "/health", method = RequestMethod.GET, produces = { "application/json", "text/json" })
    public ResponseEntity<JsonNode> statusCheck() throws JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree("{\"id\":\"api.content.health\",\"ver\":\"3.0\",\"ts\":\"2021-06-26T22:47:05Z+05:30\",\"params\":{\"resmsgid\":\"859fee0c-94d6-4a0d-b786-2025d763b78a\",\"msgid\":null,\"err\":null,\"status\":\"successful\",\"errmsg\":null},\"responseCode\":\"OK\",\"result\":{\"checks\":[{\"name\":\"redis cache\",\"healthy\":true},{\"name\":\"graph db\",\"healthy\":true},{\"name\":\"cassandra db\",\"healthy\":true}],\"healthy\":true}}");
        JsonNode resultNode = json.get("result");
        
        /* Cassandra health info */
        CassandraHealthIndicator indicator = new CassandraHealthIndicator(cassandraOperations);
    	String cassandraHealth = indicator.getHealth(false).getStatus().toString();
        ObjectNode cassandraNode = mapper.createObjectNode();
        cassandraNode.put("name", "cassandra db");
        cassandraNode.put("healthy", cassandraHealth.equals("UP"));
        
        /* Kafka health info */
        HealthIndicator kafkaIndicator = kafkaConfig.kafkaHealthIndicator();
        Map<String, Object> kafkaDetails = kafkaIndicator.health().getDetails();
        String kafkaHealth = kafkaIndicator.getHealth(false).getStatus().toString();
        
        ObjectNode kafkaNode = mapper.createObjectNode();
        /* Kafka Details node */
        ObjectNode kafkaDetailNode = mapper.createObjectNode();
        for(Map.Entry<String, Object> entry : kafkaDetails.entrySet()){
        	kafkaDetailNode.put(entry.getKey(), entry.getValue().toString());
        };

        kafkaNode.put("name", "kafka");
        kafkaNode.put("healthy", kafkaHealth.equals("UP"));
        kafkaNode.put("details", kafkaDetailNode);
        
        /* Campaign url health info */
        String url = "http://143.110.255.220:9999/";
        Boolean campaignStatus = botService.statusUrlCheck(url);
        ObjectNode campaignNode = mapper.createObjectNode();
        campaignNode.put("name", "campaign");
        campaignNode.put("healthy", campaignStatus);
        
        // create `ArrayNode` object
        ArrayNode arrayNode = mapper.createArrayNode();
        
        // add JSON users to array
        arrayNode.addAll(Arrays.asList(cassandraNode, kafkaNode, campaignNode));
        
        ((ObjectNode) resultNode).putArray("checks").addAll(arrayNode);
        
        return ResponseEntity.ok(json);
    }
    
    
}
