package com.uci.inbound.incoming;

import com.uci.adapter.gs.whatsapp.GSWhatsAppMessage;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping(value = "/whatsapp")
public class GupShupOptIn {
	
	@Operation(hidden = true)
    @RequestMapping(value = "/opt-in", method = RequestMethod.POST)
    public void gupShupWhatsApp(@Valid @RequestBody GSWhatsAppMessage message) throws Exception {

    }
}
