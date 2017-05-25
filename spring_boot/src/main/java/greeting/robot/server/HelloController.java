package greeting.robot.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import greeting.robot.data.api.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.edu.agh.biowiz.model.detected.PwDetectedFace;
import pl.edu.agh.biowiz.model.profile.PwFaceDescriptor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class HelloController {

    private final Logger logger = LoggerFactory.getLogger(HelloController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AnalyserService analyserService;

    @Autowired
    private DescriptorService descriptorService;

    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

    @PostMapping(value = "/uploadFile", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Result> uploadFileHandler(@RequestParam("name") String name,
                                    @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("You failed to upload " + name + " because the file was empty.");
        }
        try {
            double startTime, detectTime, descTime;
            byte[] bytes = file.getBytes();

            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            BufferedImage bufferedImage = ImageIO.read(bais);

            logger.debug("File <{}> has been successfully uploaded", name);

            startTime = System.currentTimeMillis();

            Optional<PwDetectedFace> pwDetectedFace = analyserService.detect(bufferedImage);

            detectTime = System.currentTimeMillis();

            Optional<PwFaceDescriptor> descriptor = pwDetectedFace.flatMap(face -> {
                logger.debug("Found following face on image <{}>: {}", name, face);
                return analyserService.describe(face, bufferedImage).getDescriptor();
            });

            descTime = System.currentTimeMillis();

            logger.debug("Detect: {}; Descriptor: {}; Total: {}",
                    (detectTime - startTime) / 1000,
                    (descTime - detectTime) / 1000,
                    (descTime - startTime) / 1000);

            if (descriptor.isPresent()) {
                PwFaceDescriptor pwFaceDescriptor = descriptor.get();
                logger.debug("quality: {}", pwFaceDescriptor.getQuality());
                return descriptorService.identify(pwFaceDescriptor);
            } else {
                return Collections.emptyList();
            }
        } catch (Exception e) {
            throw new RuntimeException("You failed to upload " + name, e);
        }
    }
}
