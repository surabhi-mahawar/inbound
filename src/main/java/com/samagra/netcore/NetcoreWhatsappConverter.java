package com.samagra.netcore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.samagra.Publisher.CommonProducer;
import com.samagra.adapter.netcore.whatsapp.inbound.NetcoreMessageFormat;
import com.samagra.adapter.netcore.whatsapp.NetcoreWhatsappAdapter;
import com.samagra.utils.XMsgProcessingUtil;
import lombok.extern.slf4j.Slf4j;
import messagerosa.dao.XMessageRepo;
import com.uci.utils.BotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.bind.JAXBException;

@Slf4j
@RestController
@RequestMapping(value = "/netcore")
public class NetcoreWhatsappConverter {

    @Value("${inboundProcessed}")
    private String inboundProcessed;

    @Value("${gupshup-opted-out}")
    private String optedOut;

    @Value("${inbound-error}")
    private String inboundError;

    private NetcoreWhatsappAdapter netcoreWhatsappAdapter;

    @Autowired
    public CommonProducer kafkaProducer;

    @Autowired
    public XMessageRepo xmsgRepo;

    @Autowired
    public BotService botService;

    @RequestMapping(value = "/whatsApp", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void netcoreWhatsApp(@RequestBody NetcoreMessageFormat message) throws JsonProcessingException, JAXBException {

        System.out.println(message.toString());

        netcoreWhatsappAdapter = NetcoreWhatsappAdapter.builder()
                .botservice(botService)
                .xmsgRepo(xmsgRepo)
                .build();

        XMsgProcessingUtil.builder()
                .adapter(netcoreWhatsappAdapter)
                .xMsgRepo(xmsgRepo)
                .inboundMessage(message.getMessages()[0])
                .topicFailure(inboundError)
                .topicSuccess(inboundProcessed)
                .kafkaProducer(kafkaProducer)
                .build()
                .process();
    }
}
