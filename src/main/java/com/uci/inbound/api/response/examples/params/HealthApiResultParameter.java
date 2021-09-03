package com.uci.inbound.api.response.examples.params;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public class HealthApiResultParameter {
	public List<HealthApiChecksParameter> checks;
	
	@Schema(example = "true")
	public Boolean healthy;
}
