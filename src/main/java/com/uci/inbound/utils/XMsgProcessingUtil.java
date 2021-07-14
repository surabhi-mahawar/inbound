package com.uci.inbound.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.adapter.provider.factory.AbstractProvider;
import com.uci.adapter.Request.CommonMessage;
import com.uci.dao.models.XMessageDAO;
import com.uci.dao.repository.XMessageRepository;
import com.uci.dao.utils.XMessageDAOUtills;
import com.uci.utils.kafka.SimpleProducer;
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

@Slf4j
@Builder
public class XMsgProcessingUtil {

    AbstractProvider adapter;
    CommonMessage inboundMessage;
    SimpleProducer kafkaProducer;
    XMessageRepository xMsgRepo;
    String topicSuccess;
    String topicFailure;

    public void process() throws JsonProcessingException {

        log.info("incoming message {}", new ObjectMapper().writeValueAsString(inboundMessage));
        try {
            adapter.convertMessageToXMsg(inboundMessage)
                    .doOnError(genericError("Error in converting to XMessage by Adapter"))
                    .subscribe(xmsg -> {
                        XMessageDAO currentMessageToBeInserted = XMessageDAOUtills.convertXMessageToDAO(xmsg);
                        if (isCurrentMessageReply(xmsg)) {
                            String whatsappId = xmsg.getMessageId().getChannelMessageId();
                            getLatestXMessage(xmsg.getFrom().getUserID(), XMessage.MessageState.REPLIED)
                                    .doOnError(genericError("Error in getting last message"))
                                    .subscribe(new Consumer<XMessageDAO>() {
                                        @Override
                                        public void accept(XMessageDAO previousMessage) {
                                            previousMessage.setMessageId(whatsappId);
                                            xMsgRepo.save(previousMessage)
                                                    .doOnError(genericError("Error in saving previous message"))
                                                    .subscribe(new Consumer<XMessageDAO>() {
                                                        @Override
                                                        public void accept(XMessageDAO updatedPreviousMessage) {
                                                            xMsgRepo.insert(currentMessageToBeInserted)
                                                                    .doOnError(genericError("Error in inserting current message"))
                                                                    .subscribe(insertedMessage -> {
                                                                        sendEventToKafka(xmsg);
                                                                    });
                                                        }
                                                    });
                                        }
                                    });
                        } else {
                            xMsgRepo.insert(currentMessageToBeInserted)
                                    .doOnError(genericError("Error in inserting current message"))
                                    .subscribe(xMessageDAO -> {
                                        sendEventToKafka(xmsg);
                                    });

                        }
                    });

        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    private Consumer<Throwable> genericError(String s) {
        return c -> {
            log.error(s + "::" + c.getMessage());
        };
    }

    private boolean isCurrentMessageReply(XMessage xmsg) {
        return !xmsg.getMessageState().equals(XMessage.MessageState.REPLIED);
    }

    private void sendEventToKafka(XMessage xmsg) {
        String xmessage = null;
        try {
            xmessage = xmsg.toXML();
        } catch (JAXBException e) {
            kafkaProducer.send(topicFailure, inboundMessage.toString());
        }
        kafkaProducer.send(topicSuccess, xmessage);
    }

    private Mono<XMessageDAO> getLatestXMessage(String userID, XMessage.MessageState messageState) {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1L);
        return xMsgRepo
                .findAllByFromIdAndTimestampAfter(userID, yesterday)
                .doOnError(genericError(String.format("Unable to find previous Message for userID %s", userID)))
                .collectList()
                .map(xMessageDAOS -> {
                    if (xMessageDAOS.size() > 0) {
                        List<XMessageDAO> filteredList = new ArrayList<>();
                        for (XMessageDAO xMessageDAO : xMessageDAOS) {
                            if (xMessageDAO.getMessageState().equals(messageState.name()))
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