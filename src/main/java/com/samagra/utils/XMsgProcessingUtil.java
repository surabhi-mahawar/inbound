package com.samagra.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samagra.Publisher.CommonProducer;
import com.samagra.adapter.provider.factory.AbstractProvider;
import com.samagra.common.Request.CommonMessage;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.XMessage;
import messagerosa.dao.XMessageDAO;
import messagerosa.dao.XMessageDAOUtills;
import messagerosa.dao.XMessageRepo;

import javax.xml.bind.JAXBException;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Builder
public class XMsgProcessingUtil {

    AbstractProvider adapter;
    CommonMessage inboundMessage;
    CommonProducer kafkaProducer;
    XMessageRepo xMsgRepo;
    String topicSuccess;
    String topicFailure;

    public void process() throws JsonProcessingException {

        log.info("incoming message {}", new ObjectMapper().writeValueAsString(inboundMessage));
        try {
            adapter.convertMessageToXMsg(inboundMessage).subscribe(new Consumer<XMessage>() {
                @Override
                public void accept(XMessage xmsg) {
                    try{
                        log.info("Converted");
                        XMessageDAO dao = XMessageDAOUtills.convertXMessageToDAO(xmsg);
                        String whatsappId;
                        if (!xmsg.getMessageState().equals(XMessage.MessageState.REPLIED)) {
                            whatsappId   = xmsg.getMessageId().getChannelMessageId();
                            List<XMessageDAO> xDbs = xMsgRepo.findAllByFromIdAndMessageStateOrderByTimestamp(xmsg.getFrom().getUserID(), XMessage.MessageState.REPLIED.name());

                            if (xDbs.size() > 0) {
                                log.info("last replied message {}",xDbs.get(0));
                                XMessageDAO prevMsg = xDbs.get(0);
                                prevMsg.setMessageId(whatsappId);
                                xMsgRepo.save(prevMsg);
                            }
                        }
                        xMsgRepo.save(dao);
                        String xmessage = xmsg.toXML();
                        log.info("xml {}", xmessage);
                        kafkaProducer.send(topicSuccess, xmessage);
                    }catch (JAXBException | JsonProcessingException e) {
                        log.info("exception message {}", e.getMessage());
                        try {
                            kafkaProducer.send(topicFailure, inboundMessage.toString());
                        } catch (JsonProcessingException jsonProcessingException) {
                            jsonProcessingException.printStackTrace();
                        }
                    }


                }
            });

        }catch (Exception e){

        }
    }
}
