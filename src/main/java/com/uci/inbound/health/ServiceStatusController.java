package com.uci.inbound.health;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uci.dao.models.XMessageDAO;
import com.uci.dao.repository.XMessageRepository;
import com.uci.utils.BotService;
import com.uci.dao.service.HealthService;
import com.uci.inbound.api.response.examples.ComponentHealthApiExample;
import com.uci.inbound.api.response.examples.HealthApiExample;
import com.uci.utils.kafka.KafkaConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
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
@RequestMapping(value = "/service")
@Tag(name = "Services Health Api")
public class ServiceStatusController {
	@Autowired
	private HealthService healthService;

	@Operation(summary = "Get Cassandra Health", description = "This API is used to get cassandra health status")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "OK!", content = {
			@Content(mediaType = "application/json", schema = @Schema(implementation = ComponentHealthApiExample.class)) }) })
	@RequestMapping(value = "/health/cassandra", method = RequestMethod.GET, produces = { "application/json",
			"text/json" })
	public ResponseEntity<JsonNode> cassandraStatusCheck() throws IOException, JsonProcessingException {
		JsonNode jsonNode = getResponseJsonNode();
		((ObjectNode) jsonNode).put("result", healthService.getCassandraHealthNode());

		return ResponseEntity.ok(jsonNode);
	}

	@Operation(summary = "Get Kafka Health", description = "This API is used to get kafka health status")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "OK!", content = {
			@Content(mediaType = "application/json", schema = @Schema(implementation = ComponentHealthApiExample.class)) }) })
	@RequestMapping(value = "/health/kafka", method = RequestMethod.GET, produces = { "application/json", "text/json" })
	public ResponseEntity<JsonNode> kafkaStatusCheck() throws IOException, JsonProcessingException {
		JsonNode jsonNode = getResponseJsonNode();
		((ObjectNode) jsonNode).put("result", healthService.getKafkaHealthNode());

		return ResponseEntity.ok(jsonNode);
	}

	@Operation(summary = "Get Campaign Health", description = "This API is used to get campaign health status")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "OK!", content = {
			@Content(mediaType = "application/json", schema = @Schema(implementation = ComponentHealthApiExample.class)) }) })
	@RequestMapping(value = "/health/campaign", method = RequestMethod.GET, produces = { "application/json",
			"text/json" })
	public ResponseEntity<JsonNode> campaignUrlStatusCheck() throws JsonProcessingException, IOException {
		JsonNode jsonNode = getResponseJsonNode();
		((ObjectNode) jsonNode).put("result", healthService.getCampaignUrlHealthNode());

		return ResponseEntity.ok(jsonNode);
	}

	/**
	 * Returns json node for service response
	 * 
	 * @return JsonNode
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 */
	private JsonNode getResponseJsonNode() throws JsonMappingException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonNode = mapper.readTree(
				"{\"id\":\"api.content.service.health\",\"ver\":\"3.0\",\"ts\":null,\"params\":{\"resmsgid\":null,\"msgid\":null,\"err\":null,\"status\":\"successful\",\"errmsg\":null},\"responseCode\":\"OK\",\"result\":{\"healthy\":false}}");
		return jsonNode;
	}
}
