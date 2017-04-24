package greeting.robot.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.edu.agh.biowiz.face.lib.pw.PwFaceAnalysisLib;
import pl.edu.agh.biowiz.model.detected.ImageRectangle;
import pl.edu.agh.biowiz.model.detected.PwDetectedFace;
import pl.edu.agh.biowiz.model.profile.PwFaceDescriptor;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Optional;

@RestController
public class HelloController {

    private final Logger logger = LoggerFactory.getLogger(HelloController.class);

    @Autowired
    private AnalyserService analyserService;

    @Autowired
    private DescriptorService descriptorService;

    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

    @RequestMapping(value = "/uploadFile", method = RequestMethod.POST)
    @ResponseBody
    public String uploadFileHandler(@RequestParam("name") String name,
                                    @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return "You failed to upload " + name + " because the file was empty.";
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
                return String.join("\n", descriptorService.identify(pwFaceDescriptor));
            } else {
                return "No faces found";
            }
        } catch (Exception e) {
            return "You failed to upload " + name + " => " + e.getMessage();
        }
    }
}
