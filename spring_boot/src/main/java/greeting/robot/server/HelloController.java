package greeting.robot.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import greeting.robot.data.api.ErrorResult;
import greeting.robot.data.api.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.edu.agh.biowiz.model.detected.PwDetectedFace;
import pl.edu.agh.biowiz.model.profile.PwFaceDescriptor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RestController
public class HelloController {

    private final Logger logger = LoggerFactory.getLogger(HelloController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String EMPTY_RESPONSE = "[]";

    @Autowired
    private AnalyserService analyserService;

    @Autowired
    private DescriptorService descriptorService;

    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

    @RequestMapping(value = "/uploadFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String uploadFileHandler(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            logger.warn("You failed to upload file because the it was empty.");
            return EMPTY_RESPONSE;
        }
        try {
            double startTime, detectTime, descTime;
            byte[] bytes = file.getBytes();

            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            BufferedImage bufferedImage = ImageIO.read(bais);

            logger.debug("File has been successfully uploaded");

            startTime = System.currentTimeMillis();

            Optional<PwDetectedFace> pwDetectedFace = analyserService.detect(bufferedImage);

            detectTime = System.currentTimeMillis();

            Optional<PwFaceDescriptor> descriptor = pwDetectedFace.flatMap(face -> {
                logger.debug("Found following face on image: {}", face);
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
                List<Result> detectedFaces = descriptorService.identify(pwFaceDescriptor);
                detectedFaces.sort(Comparator.reverseOrder());
                int endIndex = detectedFaces.size() > 3 ? 3 : detectedFaces.size();
                String result = objectMapper.writeValueAsString(detectedFaces.subList(0, endIndex));
                logger.debug("Returning following detections: {}", result);
                return result;
            } else {
                logger.warn("No faces found");
                ErrorResult errorResult = new ErrorResult();
                errorResult.setMessage("No faces found");
                return objectMapper.writeValueAsString(errorResult);
            }
        } catch (Exception e) {
            logger.error("Error occurred", e);
            ErrorResult errorResult = new ErrorResult();
            errorResult.setMessage(e.getMessage());
            try {
                return objectMapper.writeValueAsString(errorResult);
            } catch (JsonProcessingException ex) {
                logger.error("", ex);
                return EMPTY_RESPONSE;
            }
        }
    }

    @RequestMapping(value = "/createDescriptor", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void createDescriptor(@RequestParam("name") String name,
                                 @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            logger.warn("Creating descriptor for " + name + " failed due to empty content.");
            return;
        }
        try {
            byte[] bytes = file.getBytes();

            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            BufferedImage bufferedImage = ImageIO.read(bais);

            logger.debug("File <{}> has been successfully uploaded", name);

            Optional<PwFaceDescriptor> desc = analyserService.getDescriptorFor(bufferedImage);

            if (desc.isPresent()) {
                PwFaceDescriptor descriptor = desc.get();
                try (FileOutputStream fos = new FileOutputStream(new File(name + ".txt"))) {
                    fos.write(Arrays.toString(descriptor.getDescriptor()).getBytes());
                }
            } else {
                logger.warn("Descriptor could not be created");
            }
        } catch (Exception e) {
            logger.error("Creating descriptor for " + name + " failed => " + e.getMessage());
        }
    }
}
