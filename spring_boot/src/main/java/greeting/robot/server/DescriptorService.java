package greeting.robot.server;

import org.springframework.stereotype.Service;
import pl.edu.agh.biowiz.model.profile.PwFaceDescriptor;

import java.util.Collections;
import java.util.List;

@Service
public class DescriptorService {

//    private AnalyserService analyserService;

//    @Autowired
//    public DescriptorService(AnalyserService analyserService) {
//        this.analyserService = analyserService;
//    }

    public List<String> identify(PwFaceDescriptor descriptor) {
        return Collections.singletonList("Paweł Maniecki");
    }
}
