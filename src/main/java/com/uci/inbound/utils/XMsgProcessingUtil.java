package com.uci.inbound.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.adapter.provider.factory.AbstractProvider;
import com.uci.adapter.Request.CommonMessage;
import com.uci.dao.models.XMessageDAO;
import com.uci.dao.repository.XMessageRepository;
import com.uci.dao.utils.XMessageDAOUtils;
import com.uci.utils.BotService;
import com.uci.utils.kafka.SimpleProducer;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.SenderReceiverInfo;
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
    SimpleProducer kafkaProducer;
    XMessageRepository xMsgRepo;
    String topicSuccess;
    String topicFailure;
    BotService botService;


    public void process() throws JsonProcessingException {

        log.info("incoming message {}", new ObjectMapper().writeValueAsString(inboundMessage));
        try {
            adapter.convertMessageToXMsg(inboundMessage)
                    .doOnError(genericError("Error in converting to XMessage by Adapter"))
                    .subscribe(xmsg -> {
                        getAppName(xmsg.getPayload().getText(), xmsg.getFrom())
                                .subscribe(appName -> {
                                    xmsg.setApp(appName);
                                    XMessageDAO currentMessageToBeInserted = XMessageDAOUtils.convertXMessageToDAO(xmsg);
                                    if (isCurrentMessageNotAReply(xmsg)) {
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
                                                                                    log.info("Message Persisted in Cassandra 1");
                                                                                    sendEventToKafka(xmsg);
                                                                                });
                                                                    }
                                                                });
                                                    }
                                                });
                                    } else {
                                        xMsgRepo.insert(currentMessageToBeInserted)
                                                .log()
                                                .doOnError(genericError("Error in inserting current message"))
                                                .subscribe(xMessageDAO -> {
                                                    log.info("Message Persisted in Cassandra 2");
                                                    sendEventToKafka(xmsg);
                                                });
                                    }
                                });

                    });

        } catch (JAXBException e) {
        	log.info("Error Message: "+e.getMessage());
            e.printStackTrace();
        }
    }

    private Consumer<Throwable> genericError(String s) {
        return c -> {
            log.error(s + "::" + c.getMessage());
        };
    }

    private boolean isCurrentMessageNotAReply(XMessage xmsg) {
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

    private Mono<String> getAppName(String text, SenderReceiverInfo from) {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1L);
        if (text.equals("")) {
            try {
                return getLatestXMessage(from.getUserID(), yesterday, XMessage.MessageState.SENT.name()).map(new Function<XMessageDAO, String>() {
                    @Override
                    public String apply(XMessageDAO xMessageLast) {
                        return xMessageLast.getApp();
                    }
                }).doOnError(genericError("Error in getting latest xmessage"));
            } catch (Exception e2) {
                return getLatestXMessage(from.getUserID(), yesterday, XMessage.MessageState.SENT.name()).map(new Function<XMessageDAO, String>() {
                    @Override
                    public String apply(XMessageDAO xMessageLast) {
                        return xMessageLast.getApp();
                    }
                }).doOnError(genericError("Error in getting latest xmessage - catch"));
            }
        } else {
            try {
            	log.error("getCampaignFromStartingMessage text: "+text);
                return botService.getCampaignFromStartingMessage(text)
                        .flatMap(new Function<String, Mono<? extends String>>() {
                            @Override
                            public Mono<String> apply(String appName1) {
                                if (appName1 == null || appName1.equals("")) {
                                    try {
                                        return getLatestXMessage(from.getUserID(), yesterday, XMessage.MessageState.SENT.name()).map(new Function<XMessageDAO, String>() {
                                            @Override
                                            public String apply(XMessageDAO xMessageLast) {
                                                return (xMessageLast.getApp() == null || xMessageLast.getApp().isEmpty()) ? "finalAppName" : xMessageLast.getApp();
                                            }
                                        }).doOnError(genericError("Error in getting latest xmessage when app name empty"));
                                    } catch (Exception e2) {
                                        return getLatestXMessage(from.getUserID(), yesterday, XMessage.MessageState.SENT.name()).map(new Function<XMessageDAO, String>() {
                                            @Override
                                            public String apply(XMessageDAO xMessageLast) {
                                                return (xMessageLast.getApp() == null || xMessageLast.getApp().isEmpty()) ? "finalAppName" : xMessageLast.getApp();
                                            }
                                        }).doOnError(genericError("Error in getting latest xmessage when app name empty - catch"));
                                    }
                                }
                                return (appName1 == null || appName1.isEmpty()) ? Mono.just("finalAppName") : Mono.just(appName1);
                            }
                        });
            } catch (Exception e) {
            	log.error("Exception in getCampaignFromStartingMessage :"+e.getMessage());
                try {
                    return getLatestXMessage(from.getUserID(), yesterday, XMessage.MessageState.SENT.name()).map(new Function<XMessageDAO, String>() {
                        @Override
                        public String apply(XMessageDAO xMessageLast) {
                            return xMessageLast.getApp();
                        }
                    }).doOnError(genericError("Error in getting latest xmessage when exception in getCampaignFromStartingMessage"));
                } catch (Exception e2) {
                    return getLatestXMessage(from.getUserID(), yesterday, XMessage.MessageState.SENT.name()).map(new Function<XMessageDAO, String>() {
                        @Override
                        public String apply(XMessageDAO xMessageLast) {
                            return xMessageLast.getApp();
                        }
                    }).doOnError(genericError("Error in getting latest xmessage when exception in getCampaignFromStartingMessage - catch"));
                }
            }
        }
    }

    private Mono<XMessageDAO> getLatestXMessage(String userID, LocalDateTime yesterday, String messageState) {
        return xMsgRepo.findAllByUserIdAndTimestampAfter(userID, yesterday)
                .collectList()
                .map(new Function<List<XMessageDAO>, XMessageDAO>() {
                    @Override
                    public XMessageDAO apply(List<XMessageDAO> xMessageDAOS) {
                        if (xMessageDAOS.size() > 0) {
                            List<XMessageDAO> filteredList = new ArrayList<>();
                            for (XMessageDAO xMessageDAO : xMessageDAOS) {
                                if (xMessageDAO.getMessageState().equals(XMessage.MessageState.SENT.name())
                                        || xMessageDAO.getMessageState().equals(XMessage.MessageState.REPLIED.name()))
                                    filteredList.add(xMessageDAO);
                            }
                            if (filteredList.size() > 0) {
                                filteredList.sort(new Comparator<XMessageDAO>() {
                                    @Override
                                    public int compare(XMessageDAO o1, XMessageDAO o2) {
                                        return o1.getTimestamp().compareTo(o2.getTimestamp());
                                    }
                                });
                            }
                            return xMessageDAOS.get(0);
                        }
                        return new XMessageDAO();
                    }
                });
    }
}