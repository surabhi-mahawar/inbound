package com.samagra.gupshup.incoming;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.samagra.adapter.gs.whatsapp.GSWhatsAppMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.xml.bind.JAXBException;

@Slf4j
@RestController
@RequestMapping(value = "/whatsapp")
public class GupShupOptIn {
    @RequestMapping(value = "/opt-in", method = RequestMethod.POST)
    public void gupShupWhatsApp(@Valid @RequestBody GSWhatsAppMessage message) throws Exception {

    }
}
