package com.uci.inbound.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.adapter.provider.factory.AbstractProvider;
import com.uci.adapter.Request.CommonMessage;
import com.uci.dao.models.XMessageDAO;
import com.uci.dao.repository.XMessageRepository;
import com.uci.dao.utils.XMessageDAOUtills;
import com.uci.utils.CommonProducer;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.XMessage;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Builder
public class XMsgProcessingUtil {

    AbstractProvider adapter;
    CommonMessage inboundMessage;
    CommonProducer kafkaProducer;
    XMessageRepository xMsgRepo;
    String topicSuccess;
    String topicFailure;

    public void process() throws JsonProcessingException {

        log.info("incoming message {}", new ObjectMapper().writeValueAsString(inboundMessage));
        try {
            adapter.convertMessageToXMsg(inboundMessage).subscribe(xmsg -> {
                    log.info("Converted");
                    XMessageDAO dao = XMessageDAOUtills.convertXMessageToDAO(xmsg);
                    String whatsappId;
                    if (!xmsg.getMessageState().equals(XMessage.MessageState.REPLIED)) {
                        whatsappId  = xmsg.getMessageId().getChannelMessageId();
                        getLatestXMessage(xmsg.getFrom().getUserID()).subscribe(new Consumer<XMessageDAO>() {
                            @Override
                            public void accept(XMessageDAO xMessageDAO) {
                                if(xMessageDAO.getId() != null){
                                    xMessageDAO.setMessageId(whatsappId);
                                    xMsgRepo.insert(xMessageDAO).subscribe(xMessage -> xMsgRepo.insert(dao).subscribe(xMessageDAO1 -> sendEventToKafka(xmsg)));
                                }else{
                                    xMsgRepo.insert(dao).subscribe(xMessage -> sendEventToKafka(xmsg));
                                }
                            }
                        });
                    }else{
                        xMsgRepo.insert(dao).subscribe(xMessageDAO -> {
                           sendEventToKafka(xmsg);
                        });

                    }
            });

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void sendEventToKafka(XMessage xmsg) {
        String xmessage = null;
        try {
            xmessage = xmsg.toXML();
        } catch (JAXBException e) {
            try {
                kafkaProducer.send(topicFailure, inboundMessage.toString());
            } catch (JsonProcessingException jsonProcessingException) {
                jsonProcessingException.printStackTrace();
            }
        }
        log.info("xml {}", xmessage);
        try {
            kafkaProducer.send(topicSuccess, xmessage);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }


    private Mono<XMessageDAO> getLatestXMessage(String userID) {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1L);
        return xMsgRepo.findAllByFromIdAndTimestampAfter(userID, yesterday).collectList().map(xMessageDAOS -> {
            if (xMessageDAOS.size() > 0) {
                List<XMessageDAO> filteredList = new ArrayList<>();
                for (XMessageDAO xMessageDAO : xMessageDAOS) {
                    if (xMessageDAO.getMessageState().equals(XMessage.MessageState.REPLIED.name()))
                        filteredList.add(xMessageDAO);
                }
                if (filteredList.size() > 0) {
                    filteredList.sort(Comparator.comparing(XMessageDAO::getTimestamp));
                }

                return xMessageDAOS.get(0);
            }
            return new XMessageDAO();
        });
    }
}