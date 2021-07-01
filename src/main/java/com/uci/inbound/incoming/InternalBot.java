package com.uci.inbound.incoming;


import com.samagra.adapter.provider.factory.ProviderFactory;
import com.uci.utils.CommonProducer;
import io.fusionauth.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "/internalBot")
public class InternalBot {
    @Value("${campaign}")
    private String campaign;

    @Autowired
    public CommonProducer kafkaProducer;

    @Qualifier("rest")
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ProviderFactory factoryProvider;

    @RequestMapping(value = "/delete-leave", method = RequestMethod.GET)
    public ResponseEntity<User> deleteLeave(
            @RequestParam(value = "userEmail", required = false) String userEmail,
            @RequestParam(value = "workingDays", required = false) int workingDays) {

        // String url = String.format("http://transformer:9091/delete-leave?userEmail=%s&workingDays=%s", userEmail, workingDays);
        String url = String.format("http://transformer:9091/delete-leave?userEmail=%s&workingDays=%s", userEmail, workingDays);
        return restTemplate.getForEntity(url, User.class);
    }

    @RequestMapping(value = "/approve-leave", method = RequestMethod.GET)
    public ResponseEntity<User> approveLeave(
            @RequestParam(value = "userEmail", required = false) String userEmail,
            @RequestParam(value = "workingDays", required = false) int workingDays) {

        // String url = String.format("http://transformer:9091/delete-leave?userEmail=%s&workingDays=%s", userEmail, workingDays);
        String url = String.format("http://transformer:9091/approve-leave?userEmail=%s&workingDays=%s", userEmail, workingDays);
        return restTemplate.getForEntity(url, User.class);
    }

    @RequestMapping(value = "/reject-leave", method = RequestMethod.GET)
    public ResponseEntity<User> rejectLeave(
            @RequestParam(value = "userEmail", required = false) String userEmail,
            @RequestParam(value = "workingDays", required = false) int workingDays) {

        // String url = String.format("http://transformer:9091/delete-leave?userEmail=%s&workingDays=%s", userEmail, workingDays);
        String url = String.format("http://transformer:9091/reject-leave?userEmail=%s&workingDays=%s", userEmail, workingDays);
        return restTemplate.getForEntity(url, User.class);
    }
}