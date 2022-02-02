package com.uci.inbound.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uci.adapter.provider.factory.AbstractProvider;
import com.uci.adapter.Request.CommonMessage;
import com.uci.dao.models.XMessageDAO;
import com.uci.dao.repository.XMessageRepository;
import com.uci.dao.utils.XMessageDAOUtils;
import com.uci.utils.BotService;
import com.uci.utils.bot.util.BotUtil;
import com.uci.utils.kafka.SimpleProducer;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import messagerosa.core.model.XMessagePayload;
import messagerosa.xml.XMessageParser;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    String topicOutbound;
    BotService botService;


    public void process() throws JsonProcessingException {

        log.info("incoming message {}", new ObjectMapper().writeValueAsString(inboundMessage));
        try {
            adapter.convertMessageToXMsg(inboundMessage)
                    .doOnError(genericError("Error in converting to XMessage by Adapter"))
                    .subscribe(xmsg -> {
                        getAppName(xmsg.getPayload().getText(), xmsg.getFrom())
                                .subscribe(resultPair -> {
                                	/* If bot is invalid, send error message to outbound, else process message */
                                    if(!resultPair.getLeft()) {
                                    	ObjectNode botNode = (ObjectNode) ((Pair<Object, String>) resultPair.getRight()).getLeft();
                                    	String message = ((Pair<Object, String>) resultPair.getRight()).getRight().toString();
                                    	xmsg.setApp(botNode.path("result").path("data").path("name").asText());
                                    	XMessageDAO currentMessageToBeInserted = XMessageDAOUtils.convertXMessageToDAO(xmsg);
                                    	xMsgRepo.insert(currentMessageToBeInserted)
	                                        .doOnError(genericError("Error in inserting current message"))
	                                        .subscribe(xMessageDAO -> {
	                                        	SenderReceiverInfo to = SenderReceiverInfo.builder().userID(xmsg.getFrom().getUserID()).build();
	                                        	xmsg.setTo(to);
	                                        	xmsg.setAdapterId((((JsonNode) ((ArrayNode) botNode.path("result").path("data").path("logic"))).get(0).path("adapter")).asText());
	                                        	XMessagePayload payload = XMessagePayload.builder().text(message).build();
	                                        	xmsg.setPayload(payload);
	                                        	sendEventToOutboundKafka(xmsg);
	                                        });
                                    } else {
                                    	String appName = resultPair.getRight().toString();
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
    
    private void sendEventToOutboundKafka(XMessage xmsg) {
        String xmessage = null;
        try {
            xmessage = xmsg.toXML();
            log.info("xmessage: "+xmessage);
        } catch (JAXBException e) {
//            kafkaProducer.send(topicFailure, inboundMessage.toString());
        }
        kafkaProducer.send(topicOutbound, xmessage);
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

    private Mono<Pair<Boolean, Object>> getAppName(String text, SenderReceiverInfo from) {
    	LocalDateTime yesterday = LocalDateTime.now().minusDays(1L);
        if (text.equals("")) {
            try {
                return getLatestXMessage(from.getUserID(), yesterday, XMessage.MessageState.SENT.name()).map(new Function<XMessageDAO, Pair<Boolean, Object>>() {
                    @Override
                    public Pair<Boolean, Object> apply(XMessageDAO xMessageLast) {
                    	return Pair.of(true, xMessageLast.getApp());
                    }
                }).doOnError(genericError("Error in getting latest xmessage"));
            } catch (Exception e2) {
                return getLatestXMessage(from.getUserID(), yesterday, XMessage.MessageState.SENT.name()).map(new Function<XMessageDAO, Pair<Boolean, Object>>() {
                    @Override
                    public Pair<Boolean, Object> apply(XMessageDAO xMessageLast) {
                        return Pair.of(true, xMessageLast.getApp());
                    }
                }).doOnError(genericError("Error in getting latest xmessage - catch"));
            }
        } else {
            try {
            	log.error("getCampaignFromStartingMessage text: "+text);
                return botService.getBotFromStartingMessage(text)
                		.flatMap(new Function<JsonNode, Mono<? extends Pair<Boolean, Object>>>() {
                            @Override
                            public Mono<Pair<Boolean, Object>> apply(JsonNode botNode) {
                            	log.info("botNode:"+botNode);
                            	String appName1 = null;
                            	if(botNode != null && !botNode.path("result").isEmpty()) {
                            		String botValid= BotUtil.getBotValidFromJsonNode(botNode);
                                	if(!botValid.equals("true")) {
                                		return Mono.just(Pair.of(false, Pair.of(botNode, botValid)));
    								}
                                	JsonNode name = botNode.path("result").path("data").path("name");
    								appName1 = name.asText();
                            	} else {
                            		appName1 = null;
                            	}
                            	if (appName1 == null || appName1.equals("")) {
                                    try {
                                        return getLatestXMessage(from.getUserID(), yesterday, XMessage.MessageState.SENT.name()).map(new Function<XMessageDAO, Pair<Boolean, Object>>() {
                                            @Override
                                            public Pair<Boolean, Object> apply(XMessageDAO xMessageLast) {
                                                return Pair.of(true, (xMessageLast.getApp() == null || xMessageLast.getApp().isEmpty()) ? "finalAppName" : xMessageLast.getApp());
                                            }
                                        }).doOnError(genericError("Error in getting latest xmessage when app name empty"));
                                    } catch (Exception e2) {
                                        return getLatestXMessage(from.getUserID(), yesterday, XMessage.MessageState.SENT.name()).map(new Function<XMessageDAO, Pair<Boolean, Object>>() {
                                            @Override
                                            public Pair<Boolean, Object> apply(XMessageDAO xMessageLast) {
                                                return Pair.of(true, (xMessageLast.getApp() == null || xMessageLast.getApp().isEmpty()) ? "finalAppName" : xMessageLast.getApp());
                                            }
                                        }).doOnError(genericError("Error in getting latest xmessage when app name empty - catch"));
                                    }
                                }
                                return Mono.just(Pair.of(true, (appName1 == null || appName1.isEmpty()) ? "finalAppName" : appName1));
                            }
                        });
            } catch (Exception e) {
            	log.error("Exception in getCampaignFromStartingMessage :"+e.getMessage());
                try {
                    return getLatestXMessage(from.getUserID(), yesterday, XMessage.MessageState.SENT.name()).map(new Function<XMessageDAO, Pair<Boolean, Object>>() {
                        @Override
                        public Pair<Boolean, Object> apply(XMessageDAO xMessageLast) {
                            return Pair.of(true, xMessageLast.getApp());
                        }
                    }).doOnError(genericError("Error in getting latest xmessage when exception in getCampaignFromStartingMessage"));
                } catch (Exception e2) {
                    return getLatestXMessage(from.getUserID(), yesterday, XMessage.MessageState.SENT.name()).map(new Function<XMessageDAO, Pair<Boolean, Object>>() {
                        @Override
                        public Pair<Boolean, Object> apply(XMessageDAO xMessageLast) {
                            return Pair.of(true, xMessageLast.getApp());
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
                                if (xMessageDAO.getMessageState().equals(XMessage.MessageState.SENT.name()))
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