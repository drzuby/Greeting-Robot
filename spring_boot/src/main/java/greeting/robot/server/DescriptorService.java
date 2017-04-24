package greeting.robot.server;

import greeting.robot.data.api.Result;
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

    public List<Result> identify(PwFaceDescriptor descriptor) {
        Result tmpResult = new Result();
        tmpResult.setFirstName("Paweł");
        tmpResult.setLastName("Maniecki");
        return Collections.singletonList(tmpResult);
    }
}
