package com.uci.inbound.api.response.examples.params;

import io.swagger.v3.oas.annotations.media.Schema;

public class HealthApiChecksParameter {
	@Schema(example = "Cassandra")
	public String name;
	
	@Schema(example = "true")
	public Boolean healthy;
}
