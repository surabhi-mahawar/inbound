package com.uci.inbound.health;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uci.dao.service.HealthService;
import com.uci.utils.cdn.samagra.MinioClientService;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobProperties;

@Slf4j
@RestController
public class HealthController {
	
	@Autowired
	private HealthService healthService;
	
	@Autowired
	private MinioClientService minioClientService;
	
    @RequestMapping(value = "/health", method = RequestMethod.GET, produces = { "application/json", "text/json" })
    public ResponseEntity<JsonNode> statusCheck() throws JsonProcessingException, IOException {
        log.error("Health API called");
        ObjectMapper mapper = new ObjectMapper();
        /* Current Date Time */
        LocalDateTime localNow = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String dateString = fmt.format(localNow).toString();
        
        String signedUrl = minioClientService.getCdnSignedUrl("bot-audio.mp3");
        log.info("signedUrl: "+signedUrl);
        
        
        JsonNode jsonNode = mapper.readTree("{\"id\":\"api.content.health\",\"ver\":\"3.0\",\"ts\":\"2021-06-26T22:47:05Z+05:30\",\"params\":{\"resmsgid\":\"859fee0c-94d6-4a0d-b786-2025d763b78a\",\"msgid\":null,\"err\":null,\"status\":\"successful\",\"errmsg\":null},\"responseCode\":\"OK\",\"result\":{\"checks\":[{\"name\":\"redis cache\",\"healthy\":true},{\"name\":\"graph db\",\"healthy\":true},{\"name\":\"cassandra db\",\"healthy\":true}],\"healthy\":true}}");
        
//        ((ObjectNode) jsonNode).put("ts", dateString);
//        ((ObjectNode) jsonNode).put("result", healthService.getAllHealthNode());
        
        return ResponseEntity.ok(jsonNode);
    }
    
    @Value("${blob.connection-string}")
    String connectionString;

    @Value("${blob.container-name}")
    String containerName;
    
    
    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public ResponseEntity<ByteArrayResource> test() {
    	BlobClientBuilder client = new BlobClientBuilder();
    	client
//    		.containerName("telemetry-data-store")
    		.endpoint("https://sunbirddevprivate.blob.core.windows.net/telemetry-data-store")
    		.sasToken("sp=rli&st=2022-02-02T07:39:11Z&se=2023-02-02T15:39:11Z&spr=https&sv=2020-08-04&sr=c&sig=rVB0GhITQOp4teflbleiUjjpT6Dvx3bDJ3AtnJVUto0%3D");
//        client.connectionString(connectionString);
//        client.containerName(containerName);
//        
//        BlobClient client1 = new BlobClient(new Uri(""));
    	
    	
        
        String file = "failed/2021-04-21-1620300743983.json.gz";
        byte[] data = getFile(client, file);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity
                .ok()
                .contentLength(data.length)
                .header("Content-type", "application/octet-stream")
                .header("Content-disposition", "attachment; filename=\"" + file + "\"")
                .body(resource);
    }
    
    public byte[] getFile(BlobClientBuilder client, String name) {
        try {
            File temp = new File(name);
            BlobProperties properties = client.blobName(name).buildClient().downloadToFile(temp.getPath());
            byte[] content = Files.readAllBytes(Paths.get(temp.getPath()));
            temp.delete();
            return content;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
