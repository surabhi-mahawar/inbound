package com.uci.inbound.health;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.uci.dao.service.HealthService;
import com.uci.utils.cdn.samagra.MinioClientService;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.apache.commons.compress.utils.FileNameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    
//    @Value("${blob.connection-string}")
//    String connectionString;
//
//    @Value("${blob.container-name}")
//    String containerName;
//    
//    
//    @RequestMapping(value = "/test", method = RequestMethod.GET)
//    public ResponseEntity<ByteArrayResource> test() {
//    	BlobClientBuilder client = new BlobClientBuilder();
//    	client
//    		.endpoint("https://ucitest.blob.core.windows.net/uci-test-container")
//    		.sasToken("sp=racwdl&st=2022-03-08T10:41:05Z&se=2023-01-31T18:41:05Z&spr=https&sv=2020-08-04&sr=c&sig=vYkR0e4nYLK9jQg%2FlpowpeuHzWnvYpaEcMyAe9LCIdQ%3D");
//        
//        String file = "testing-1.jpg";
//        byte[] data = getFile(client, file);
//        ByteArrayResource resource = new ByteArrayResource(data);
//
//        return ResponseEntity
//                .ok()
//                .contentLength(data.length)
//                .header("Content-type", "application/octet-stream")
//                .header("Content-disposition", "attachment; filename=\"" + file + "\"")
//                .body(resource);
//    }
//    
//    public byte[] getFile(BlobClientBuilder client, String name) {
//        try {
//            File temp = new File(name);
//            BlobProperties properties = client.blobName(name).buildClient().downloadToFile(temp.getPath());
//            byte[] content = Files.readAllBytes(Paths.get(temp.getPath()));
//            temp.delete();
//            return content;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
    
//    public URI uploadPicture(MultipartFile multipartFile) {
//        URI uri;
//        String multipartName = multipartFile.getName().replaceAll("[\n|\r|\t]", "_");
//        log.info("Profile image uploading, image name: {}", multipartName);
//
//        try {
////        	BlobStorageService.
//            String extension = FileNameUtils.getExtension(multipartFile.getOriginalFilename());
//            String fileName = String.join(".", UUID.randomUUID().toString(), extension);
//            CloudBlockBlob blob = cloudBlobContainer.getBlockBlobReference(fileName);
//            blob.upload(multipartFile.getInputStream(), -1);
//            uri = blob.getUri();
//        } catch (URISyntaxException | StorageException | IOException e) {
//            log.error("upload picture exception: "+e.getMessage());
//            return null;
//        }
////        return Optional.ofNullable(uri).orElseThrow(UploadPictureFailedException::new);
//        return uri;
//    }
}
