package com.samagra.cdac;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.samagra.Publisher.CommonProducer;
import com.samagra.adapter.cdac.CdacBulkSmsAdapter;
import com.samagra.adapter.gs.whatsapp.GSWhatsAppMessage;
import com.samagra.adapter.gs.whatsapp.GupShupWhatsappAdapter;
import com.samagra.common.Request.CommonMessage;
import com.samagra.utils.XMsgProcessingUtil;
import lombok.extern.slf4j.Slf4j;
import messagerosa.dao.XMessageRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.xml.bind.JAXBException;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "/cdac")
public class CDACConverter {

    @Value("${inboundProcessed}")
    private String inboundProcessed;

    @Value("${gupshup-opted-out}")
    private String optedOut;

    @Value("${inbound-error}")
    private String inboundError;

    @Autowired
    @Qualifier("cdacBulkSmsAdapter")
    private CdacBulkSmsAdapter cdacBulkSmsAdapter;

    @Autowired
    public CommonProducer kafkaProducer;

    @Autowired
    public XMessageRepo xmsgRepo;

    @RequestMapping(value = "/sms/bulk/", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void cdacBulk(@Valid CommonMessage message) throws JsonProcessingException {

        XMsgProcessingUtil.builder()
                .adapter(cdacBulkSmsAdapter)
                .xMsgRepo(xmsgRepo)
                .inboundMessage(message)
                .topicFailure(inboundError)
                .topicSuccess(inboundProcessed)
                .kafkaProducer(kafkaProducer)
                .build().process();
    }

    @RequestMapping(value = "/sms/single/", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void cdacSingle(@Valid CommonMessage message) throws JsonProcessingException {

        XMsgProcessingUtil.builder()
                .adapter(cdacBulkSmsAdapter)
                .xMsgRepo(xmsgRepo)
                .inboundMessage(message)
                .topicFailure(inboundError)
                .topicSuccess(inboundProcessed)
                .kafkaProducer(kafkaProducer)
                .build().process();
    }
}
