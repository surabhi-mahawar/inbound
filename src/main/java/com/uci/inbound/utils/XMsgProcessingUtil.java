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

import javax.xml.bind.JAXBException;
import java.util.List;
import java.util.function.Consumer;

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
            adapter.convertMessageToXMsg(inboundMessage).subscribe(new Consumer<XMessage>() {
                @Override
                public void accept(XMessage xmsg) {
                        log.info("Converted");
                        XMessageDAO dao = XMessageDAOUtills.convertXMessageToDAO(xmsg);
                        String whatsappId;
                        if (!xmsg.getMessageState().equals(XMessage.MessageState.REPLIED)) {
                            whatsappId  = xmsg.getMessageId().getChannelMessageId();
                            xMsgRepo.findAllByFromIdAndMessageStateOrderByTimestamp(xmsg.getFrom().getUserID(),
                                    XMessage.MessageState.REPLIED.name()).subscribe(new Consumer<List<XMessageDAO>>() {
                                @Override
                                public void accept(List<XMessageDAO> xDbs) {

                                    if (xDbs.size() > 0) {
                                        log.info("last replied message {}",xDbs.get(0));
                                        XMessageDAO prevMsg = xDbs.get(0);
                                        prevMsg.setMessageId(whatsappId);
                                        xMsgRepo.insert(prevMsg).subscribe(new Consumer<XMessageDAO>() {
                                            @Override
                                            public void accept(XMessageDAO xMessageDAO) {
                                                xMsgRepo.insert(dao).subscribe(new Consumer<XMessageDAO>() {
                                                    @Override
                                                    public void accept(XMessageDAO xMessageDAO) {
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
                                                });

                                            }
                                        });

                                    }else{
                                        xMsgRepo.insert(dao).subscribe(new Consumer<XMessageDAO>() {
                                            @Override
                                            public void accept(XMessageDAO xMessageDAO) {
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
                                        });

                                    }
                                }
                            });

                        }else{
                            xMsgRepo.insert(dao).subscribe(new Consumer<XMessageDAO>() {
                                @Override
                                public void accept(XMessageDAO xMessageDAO) {
                                    String xmessage = null;
                                    try {
                                        xmessage = xmsg.toXML();
                                    } catch (JAXBException e) {
                                        e.printStackTrace();
                                    }
                                    log.info("xml {}", xmessage);
                                    try {
                                        kafkaProducer.send(topicSuccess, xmessage);
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                        }




                }
            });

        }catch (Exception e){

        }
    }
}