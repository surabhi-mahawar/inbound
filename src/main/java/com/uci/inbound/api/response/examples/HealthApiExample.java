package com.uci.inbound.api.response.examples;

import com.uci.inbound.api.response.examples.params.HealthApiParamsParameter;
import com.uci.inbound.api.response.examples.params.HealthApiResultParameter;

import io.swagger.v3.oas.annotations.media.Schema;

public class HealthApiExample {
	@Schema(example = "api.content.health")
	public String id;

	@Schema(example = "3.0")
	public String ver;
	
	@Schema(example = "2021-09-01T11:14:50Z")
	public String ts;
	
	public HealthApiParamsParameter params;
	
	@Schema(example = "OK")
	public String responseCode;
}
