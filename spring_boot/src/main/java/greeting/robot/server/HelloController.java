package greeting.robot.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.edu.agh.biowiz.face.lib.pw.PwFaceAnalysisLib;
import pl.edu.agh.biowiz.model.detected.ImageRectangle;
import pl.edu.agh.biowiz.model.detected.PwDetectedFace;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Optional;

@RestController
public class HelloController {

    private final Logger logger = LoggerFactory.getLogger(HelloController.class);
    private final PwFaceAnalysisLib analyser = new PwFaceAnalysisLib();

    @PostConstruct
    public void postConstruct() {
        analyser.initialize();
    }

    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

    @RequestMapping(value = "/uploadFile", method = RequestMethod.POST)
    @ResponseBody
    public String uploadFileHandler(@RequestParam("name") String name,
                                    @RequestParam("file") MultipartFile file) {
        if (!file.isEmpty()) {
            try {
                double startTime, detectTime, descTime;
                byte[] bytes = file.getBytes();

                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                BufferedImage bufferedImage = ImageIO.read(bais);

                logger.debug("File <{}> has been successfully uploaded", name);

                startTime = System.currentTimeMillis();

                Optional<PwDetectedFace> pwDetectedFace = analyser.detectFaceInRectangle(bufferedImage, new ImageRectangle(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight()));

                detectTime = System.currentTimeMillis();

                pwDetectedFace.flatMap(face -> {
                    logger.debug("Found following face on image <{}>: {}", name, face);
                    return analyser.createDescriptor(face, bufferedImage).getDescriptor();
                }).ifPresent(pwFaceDescriptor -> logger.debug("quality: {} {}", pwFaceDescriptor.getQuality(), Arrays.toString(pwFaceDescriptor.getDescriptor())));

                descTime = System.currentTimeMillis();

                logger.debug("Detect: {}; Descriptor: {}; Total: {}", (detectTime - startTime) / 1000, (descTime - startTime) / 1000, (detectTime + descTime - 2 * startTime) / 1000);

                return "Upload and analysis successful";
            } catch (Exception e) {
                return "You failed to upload " + name + " => " + e.getMessage();
            }
        } else {
            return "You failed to upload " + name
                    + " because the file was empty.";
        }
    }
}
