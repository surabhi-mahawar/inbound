package com.uci.inbound.incoming;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.uci.adapter.cdac.CdacBulkSmsAdapter;
import com.uci.adapter.cdac.TrackDetails;
import com.uci.adapter.provider.factory.ProviderFactory;
import com.uci.utils.kafka.SimpleProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.xml.bind.JAXBException;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "/campaign")
public class Campaign {
    @Value("${campaign}")
    private String campaign;

    @Autowired
    public SimpleProducer kafkaProducer;

    @Autowired
    private ProviderFactory factoryProvider;

    @RequestMapping(value = "/start", method = RequestMethod.GET)
    public void startCampaign(@RequestParam("campaignId") String campaignId) throws JsonProcessingException, JAXBException {
        kafkaProducer.send(campaign, campaignId);
        return;
    }

    @RequestMapping(value = "/pause", method = RequestMethod.GET)
    public void pauseCampaign(@RequestParam("campaignId") String campaignId) throws JsonProcessingException, JAXBException {
        kafkaProducer.send(campaign, campaignId);
        return;
    }

    @RequestMapping(value = "/resume", method = RequestMethod.GET)
    public void resumeCampaign(@RequestParam("campaignId") String campaignId) throws JsonProcessingException, JAXBException {
        kafkaProducer.send(campaign, campaignId);
        return;
    }

    @RequestMapping(value = "/status/cdac/bulk", method = RequestMethod.GET)
    public TrackDetails getCampaignStatus(@RequestParam("campaignId") String campaignId) {
        CdacBulkSmsAdapter iprovider = (CdacBulkSmsAdapter) factoryProvider.getProvider("cdac", "SMS", null);
        try {
             iprovider.getLastTrackingReport(campaignId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}