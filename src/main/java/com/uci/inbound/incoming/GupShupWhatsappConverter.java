package com.uci.inbound.incoming;

import javax.validation.Valid;
import javax.xml.bind.JAXBException;

import com.uci.adapter.gs.whatsapp.GupShupWhatsappAdapter;
import com.uci.adapter.netcore.whatsapp.inbound.NetcoreMessageFormat;
import com.uci.dao.repository.XMessageRepository;
import com.uci.utils.BotService;
import com.uci.inbound.utils.XMsgProcessingUtil;
import com.uci.utils.kafka.SimpleProducer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.uci.adapter.gs.whatsapp.GSWhatsAppMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = "/gupshup")
@Tag(name = "Gupshup Apis")
public class GupShupWhatsappConverter {

    @Value("${inboundProcessed}")
    private String inboundProcessed;

    @Value("${gupshup-opted-out}")
    private String optedOut;

    @Value("${inbound-error}")
    private String inboundError;

    private GupShupWhatsappAdapter gupShupWhatsappAdapter;

    @Autowired
    public SimpleProducer kafkaProducer;

    @Autowired
    public XMessageRepository xmsgRepository;

    @Autowired
    public BotService botService;

    @Operation(summary = "Send message to kafka topic via gupshup whatsapp service", description = "This API is used to get send message to inbound kafka topic received from gupshup whatsapp service.")
//    @Parameters(value = {
//    		@Parameter(name = "waNumber", examples={ @ExampleObject(name = "Text Message", description = "This is WhatsApp Business number on which the "
//    				+ "customer has sent a message", value = "919560222091")}),
//    		@Parameter(name = "mobile", examples={ @ExampleObject(name = "Text Message", description = "The phone number of the customer who has sent "
//    				+ "the message", value = "919004371797")}),
//    		@Parameter(name = "replyId", examples={ @ExampleObject(name = "Text Message", description = "The unique system identifier for the original message sent by the business to the customer, on "
//    				+ "which the customer has replied (swipe left action on"
//    				+ "WhatsApp to reply to a specific message). This is the"
//    				+ "transaction ID of the original message.", value = "3900363981641897487")}),
//    		@Parameter(name = "messageId", examples={ @ExampleObject(name = "Text Message", description = "The unique identifier for the original message sent "
//    				+ "by the business to the customer, on which the"
//    				+ "customer has replied (swipe left action on WhatsApp"
//    				+ "to reply to a specific message). This is the message"
//    				+ "ID that can be a custom value specified in the Send"
//    				+ "Message API request.", value = "custom Message ID")}),
//    		@Parameter(name = "text", examples={ @ExampleObject(name = "Text Message", description = "The text message sent by the user", value = "Hi UCI")}),
//    		@Parameter(name = "type", examples={ @ExampleObject(name = "Text Message", description = "The name of the Gupshup app to which the"
//    				+ "customer has sent a message on WhatsApp\n."
//    				+ "Must be one of : text, image, document, voice, audio, video,"
//    				+ "location, contacts", value = "text")}),
//    		@Parameter(name = "timestamp", examples={ @ExampleObject(name = "Text Message", description = "The time in unix timestamp in milliseconds when the"
//					+ "message sent by the customer was received by"
//					+ "Gupshup", value = "1564471290000")})
//    })
//    @RequestBody(content = @Content(
//    				mediaType = "application/x-www-form-urlencoded",
//					schema = @Schema(implementation = GSWhatsAppMessage.class),
//					examples = {
//							@ExampleObject(
//								name = "waNumber", description = "Request body for text messages", 
//								value = "test")
//					}
//    		)
//    )
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "OK!", content = {
			@Content }) })
	@RequestMapping(value = "/whatsApp", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void gupshupWhatsApp(@Valid GSWhatsAppMessage message) throws JsonProcessingException, JAXBException {

        gupShupWhatsappAdapter = GupShupWhatsappAdapter.builder()
                .botservice(botService)
                .xmsgRepo(xmsgRepository)
                .build();

        XMsgProcessingUtil.builder()
                .adapter(gupShupWhatsappAdapter)
                .xMsgRepo(xmsgRepository)
                .inboundMessage(message)
                .topicFailure(inboundError)
                .topicSuccess(inboundProcessed)
                .kafkaProducer(kafkaProducer)
                .build()
                .process();
    }
}
