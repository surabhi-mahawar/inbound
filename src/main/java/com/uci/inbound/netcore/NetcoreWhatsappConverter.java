package com.uci.inbound.netcore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.uci.adapter.netcore.whatsapp.inbound.NetcoreMessageFormat;
import com.uci.adapter.netcore.whatsapp.NetcoreWhatsappAdapter;
import com.uci.inbound.utils.XMsgProcessingUtil;
import com.uci.dao.repository.XMessageRepository;
import com.uci.utils.kafka.SimpleProducer;
import com.uci.utils.kafka.SimpleProducer1;

import io.opentelemetry.api.trace.Tracer;
import lombok.extern.slf4j.Slf4j;
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
    public SimpleProducer kafkaProducer;

    @Autowired
    public XMessageRepository xmsgRepo;

    @Autowired
    public BotService botService;
    
    @Autowired
    public Tracer tracer;

    @RequestMapping(value = "/whatsApp", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void netcoreWhatsApp(@RequestBody NetcoreMessageFormat message) throws JsonProcessingException, JAXBException {

    	log.info("getApp: "+message.getMessages()[0].getApp());
    	log.info("getEventType: "+message.getMessages()[0].getEventType());
    	log.info("getMessageId: "+message.getMessages()[0].getMessageId());
    	log.info("getMobile: "+message.getMessages()[0].getMobile());
    	log.info("getName: "+message.getMessages()[0].getName());
    	log.info("getReplyId: "+message.getMessages()[0].getReplyId());
    	log.info("getSource: "+message.getMessages()[0].getSource());
    	log.info("getType: "+message.getMessages()[0].getType());
    	log.info("getWaNumber: "+message.getMessages()[0].getWaNumber());
    	log.info("getText: "+message.getMessages()[0].getText());
    	log.info("getVersion: "+message.getMessages()[0].getVersion());
//    	log.info("getWaNumber: "+message.getMessages()[0].getWaNumber());
//    	log.info("getWaNumber: "+message.getMessages()[0].getWaNumber());
    	
    			System.out.println(message.toString());

        netcoreWhatsappAdapter = NetcoreWhatsappAdapter.builder()
                .botservice(botService)
                .build();

        XMsgProcessingUtil.builder()
                .adapter(netcoreWhatsappAdapter)
                .xMsgRepo(xmsgRepo)
                .inboundMessage(message.getMessages()[0])
                .topicFailure(inboundError)
                .topicSuccess(inboundProcessed)
                .kafkaProducer(kafkaProducer)
                .botService(botService)
                .tracer(tracer)
                .build()
                .process();
    }
}
