package com.uci.inbound.xmsg;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uci.dao.models.XMessageDAO;
import com.uci.dao.repository.XMessageRepository;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/xmsg")
public class XMessageController {
	@Autowired
	private XMessageRepository xMsgRepo;
	
	@Value("${xmessage.user.data.before.hours}")
	private Float xMessageUserDataBeforehours;

	@RequestMapping(value = "/user/data/{userid}", method = RequestMethod.GET, produces = { "application/json",
			"text/json" })
	public Flux<XMessageDAO> getUserData(@PathVariable("userid") String userid)
			throws JsonMappingException, JsonProcessingException {
		return xMsgRepo.findAllByUserId(userid);
	}

	@RequestMapping(value = "/user/dataBeforeHours/{userid}", method = RequestMethod.GET, produces = { "application/json",
			"text/json" })
	public Flux<XMessageDAO> getUserDataBeforeHours(@PathVariable("userid") String userid) {
		long beforeMins = (long) (xMessageUserDataBeforehours * 60);
		LocalDateTime timestamp = LocalDateTime.now().minusMinutes(beforeMins);
		
		log.info("Timestamp: "+timestamp);
		return xMsgRepo.findAllByUserIdAndTimestampAfter(userid, timestamp);
	}

	@RequestMapping(value = "/user/data/{id}", method = RequestMethod.DELETE, produces = {
			"application/json", "text/json" })
	public void deleteById(@PathVariable("id") String id) {
		xMsgRepo.findById(UUID.fromString(id)).subscribe(xMsgDao -> {
			xMsgRepo.delete(xMsgDao).subscribe();
		});
	}
	
	@RequestMapping(value = "/user/dataBeforeHours/{userid}", method = RequestMethod.DELETE, produces = {
			"application/json", "text/json" })
	public void deleteAllByUserIdBeforeHours(@PathVariable("userid") String userid) {
		long beforeMins = (long) (xMessageUserDataBeforehours * 60);
		LocalDateTime timestamp = LocalDateTime.now().minusMinutes(beforeMins);
		
		log.info("Timestamp: "+timestamp);
		xMsgRepo.findAllByUserIdAndTimestampAfter(userid, timestamp).subscribe(xMsgDao -> {
			xMsgRepo.delete(xMsgDao).subscribe();
		});
	}
	
	@RequestMapping(value = "/user/dataByUserId/{userId}", method = RequestMethod.DELETE, produces = {
			"application/json", "text/json" })
	public void deleteAllByMobile(@PathVariable("userId") String userid) {
		xMsgRepo.findAllByUserId(userid).subscribe(xMsgDao -> {
			xMsgRepo.delete(xMsgDao).subscribe();
		});
	}
}
