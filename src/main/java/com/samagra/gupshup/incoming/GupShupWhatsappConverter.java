package com.samagra.gupshup.incoming;

import javax.validation.Valid;
import javax.xml.bind.JAXBException;

import com.samagra.adapter.gs.whatsapp.GupShupWhatsappAdapter;
import com.uci.dao.repository.XMessageRepository;
import com.uci.utils.BotService;
import com.samagra.utils.XMsgProcessingUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.samagra.Publisher.CommonProducer;
import com.samagra.adapter.gs.whatsapp.GSWhatsAppMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = "/gupshup")
public class GupShupWhatsappConverter {

    @Value("${inboundProcessed}")
    private String inboundProcessed;

    @Value("${gupshup-opted-out}")
    private String optedOut;

    @Value("${inbound-error}")
    private String inboundError;

    private GupShupWhatsappAdapter gupShupWhatsappAdapter;

    @Autowired
    public CommonProducer kafkaProducer;

    @Autowired
    public XMessageRepository xmsgRepository;

    @RequestMapping(value = "/whatsApp", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void gupshupWhatsApp(@Valid GSWhatsAppMessage message) throws JsonProcessingException, JAXBException {

        BotService botService = new BotService();
        gupShupWhatsappAdapter = GupShupWhatsappAdapter.builder()
                .botservice(botService)
                .xmsgRepository(xmsgRepository)
                .build();

        XMsgProcessingUtil.builder()
                .adapter(gupShupWhatsappAdapter)
                .xMsgRepo(xmsgRepository)
                .inboundMessage(message)
                .topicFailure(inboundError)
                .topicSuccess(inboundProcessed)
                .kafkaProducer(kafkaProducer)
                .build()
                .process();
    }
}
