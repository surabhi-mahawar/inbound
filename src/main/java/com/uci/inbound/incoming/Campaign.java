package com.uci.inbound.incoming;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.uci.adapter.cdac.CdacBulkSmsAdapter;
import com.uci.adapter.cdac.TrackDetails;
import com.uci.adapter.provider.factory.ProviderFactory;
import com.uci.utils.kafka.SimpleProducer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.xml.bind.JAXBException;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "/campaign")
@Tag(name = "Campaign Apis")
public class Campaign {
    @Value("${campaign}")
    private String campaign;

    @Autowired
    public SimpleProducer kafkaProducer;

    @Autowired
    private ProviderFactory factoryProvider;

    @Operation(summary = "Start Campaign", description = "This API is used to start the campaign kafka topic."
    		+ "- The fields marked with an asterisk (*) are mandatory. They cannot be null or empty.")
	@RequestMapping(value = "/start", method = RequestMethod.GET)
    public void startCampaign(@RequestParam("campaignId") String campaignId) throws JsonProcessingException, JAXBException {
        kafkaProducer.send(campaign, campaignId);
        return;
    }

    @Operation(summary = "Pause Campaign", description = "This API is used to pause the campaign kafka topic.")
	@RequestMapping(value = "/pause", method = RequestMethod.GET)
    public void pauseCampaign(@RequestParam("campaignId") String campaignId) throws JsonProcessingException, JAXBException {
        kafkaProducer.send(campaign, campaignId);
        return;
    }

    @Operation(summary = "Resume Campaign", description = "This API is used to start campaign kafka topic.")
	@RequestMapping(value = "/resume", method = RequestMethod.GET)
    public void resumeCampaign(@RequestParam("campaignId") String campaignId) throws JsonProcessingException, JAXBException {
        kafkaProducer.send(campaign, campaignId);
        return;
    }

    @Operation(hidden = true, summary = "Get Campaign Status", description = "This API is used to get campaign status from CDAC Sms Adapter.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "OK!", content = {
			@Content }) })
	@RequestMapping(value = "/status/cdac/bulk", method = RequestMethod.GET)
    public TrackDetails getCampaignStatus(@RequestParam("campaignId") String campaignId) {
        CdacBulkSmsAdapter iprovider = (CdacBulkSmsAdapter) factoryProvider.getProvider("cdac", "SMS");
        try {
             iprovider.getLastTrackingReport(campaignId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}