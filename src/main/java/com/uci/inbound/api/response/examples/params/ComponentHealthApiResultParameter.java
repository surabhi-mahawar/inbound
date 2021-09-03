package com.uci.inbound.api.response.examples.params;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public class ComponentHealthApiResultParameter {
	@Schema(example = "true")
	public Boolean healthy;
}
