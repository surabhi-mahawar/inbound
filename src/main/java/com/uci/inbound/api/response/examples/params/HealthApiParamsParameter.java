package com.uci.inbound.api.response.examples.params;

import io.swagger.v3.oas.annotations.media.Schema;

public class HealthApiParamsParameter {
	@Schema(example = "859fee0c-94d6-4a0d-b786-2025d763b78a")
	public String resmsgid;
	
	@Schema(example = "null")
	public String msgid;
	
	@Schema(example = "null")
	public String err;
	
	@Schema(example = "Successful")
	public String status;
	
	@Schema(example = "null")
	public String errmsg;
}
