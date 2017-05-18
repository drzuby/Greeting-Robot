package greeting.robot.server;

import greeting.robot.data.api.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.edu.agh.biowiz.model.Project;
import pl.edu.agh.biowiz.model.profile.PwFaceDescriptor;
import pl.edu.agh.biowiz.repo.holder.ProjectHolder;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class DescriptorService {

    @Autowired
    private ProjectHolder projectHolder;
    private Project biowizProject;

    @PostConstruct
    public void postConstruct() {
        Optional<Project> project = projectHolder.openProject(".biowiz-data/exampleCelebrites/project.biow");
        if (project.isPresent()) {
            biowizProject = project.get();
        } else {
            throw new IllegalStateException("Cannot deserialize project");
        }
    }
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
