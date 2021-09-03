package com.uci.inbound.netcore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.uci.adapter.netcore.whatsapp.inbound.NetcoreMessageFormat;
import com.uci.adapter.gs.whatsapp.GSWhatsAppMessage;
import com.uci.adapter.netcore.whatsapp.NetcoreWhatsappAdapter;
import com.uci.inbound.utils.XMsgProcessingUtil;
import com.uci.dao.repository.XMessageRepository;
import com.uci.utils.kafka.SimpleProducer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import com.uci.utils.BotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

import javax.xml.bind.JAXBException;

@Slf4j
@RestController
@RequestMapping(value = "/netcore")
@Tag(name = "Netcore Apis")
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
	
	@Operation(summary = "Send message to kafka topic via netcore whatsapp service", description = "This API is used to get send message to inbound kafka topic received from netcore whatsapp service.")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			content = @Content(
					schema = @Schema(implementation = NetcoreMessageFormat.class),
					examples = {
					@ExampleObject(
						name = "Text Message", description = "Request body for text messages", 
						value = "{\n"
						+ "  \"messages\": [\n"
						+ "    {\n"
						+ "      \"message_id\": \"ABEGkZlgQyWAAgo-sDVSUOa9jH0z\",\n"
						+ "      \"from\": \"919960432580\",\n"
						+ "      \"received_at\": \"1567090835\",\n"
						+ "      \"context\": {\n"
						+ "      \"ncmessage_id\": null,\n"
						+ "      \"message_id\": null\n"
						+ "      },\n"
						+ "      \"message_type\": \"TEXT\",\n"
						+ "      \"text_type\": {\n"
						+ "      \"text\": \"Hi UCI\"\n"
						+ "      }\n"
						+ "    }\n"
						+ "  ]\n"
						+ "}")
//					, @ExampleObject(name = "test2", value = "test2") 
			}))
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "OK!", content = { @Content }) })
	@RequestMapping(value = "/whatsApp", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public void netcoreWhatsApp(@RequestBody NetcoreMessageFormat message)
			throws JsonProcessingException, JAXBException {

		System.out.println(message.toString());

		netcoreWhatsappAdapter = NetcoreWhatsappAdapter.builder().botservice(botService).build();

		XMsgProcessingUtil.builder().adapter(netcoreWhatsappAdapter).xMsgRepo(xmsgRepo)
				.inboundMessage(message.getMessages()[0]).topicFailure(inboundError).topicSuccess(inboundProcessed)
				.kafkaProducer(kafkaProducer).botService(botService).build().process();
	}
}
