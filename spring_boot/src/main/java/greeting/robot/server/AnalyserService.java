package greeting.robot.server;

import org.springframework.stereotype.Service;
import pl.edu.agh.biowiz.face.lib.pw.PwFaceAnalysisLib;
import pl.edu.agh.biowiz.model.detected.ImageRectangle;
import pl.edu.agh.biowiz.model.detected.PwDetectedFace;
import pl.edu.agh.biowiz.model.profile.CreateDescriptorResult;
import pl.edu.agh.biowiz.model.profile.PwFaceDescriptor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.awt.image.BufferedImage;
import java.util.Optional;

@Service
public class AnalyserService {
    private final PwFaceAnalysisLib analyser = new PwFaceAnalysisLib();

    @PostConstruct
    public void postConstruct() {
        System.setProperty("useGpu", "true");
        analyser.initialize();
    }

    @PreDestroy
    public void preDestroy() {
        analyser.free();
    }

    public Optional<PwDetectedFace> detect(BufferedImage image) {
        ImageRectangle rectangle = new ImageRectangle(0, 0, image.getWidth(), image.getHeight());
        return analyser.detectFaceInRectangle(image, rectangle);
    }

    public CreateDescriptorResult<PwFaceDescriptor> describe(PwDetectedFace detectedFace, BufferedImage image) {
        return analyser.createDescriptor(detectedFace, image);
    }

    public Optional<PwFaceDescriptor> getDescriptorFor(BufferedImage image) {
        return detect(image).flatMap(face -> describe(face, image).getDescriptor());
    }

    public float compareDescriptors(PwFaceDescriptor first, PwFaceDescriptor second) {
        return analyser.compareDescriptors(first, second);
    }
}
