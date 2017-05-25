package greeting.robot.server;

import greeting.robot.data.api.Result;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.edu.agh.biowiz.model.Project;
import pl.edu.agh.biowiz.model.Subject;
import pl.edu.agh.biowiz.model.mark.FaceMark;
import pl.edu.agh.biowiz.model.profile.PwFaceDescriptor;
import pl.edu.agh.biowiz.repo.holder.ProjectHolder;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DescriptorService {

    @Autowired
    private AnalyserService analyserService;

    @Autowired
    private ProjectHolder projectHolder;
    private Project biowizProject;
    private List<FaceMark> faceMarks;

    @PostConstruct
    public void postConstruct() {
        biowizProject = projectHolder.openProject(".biowiz-data/exampleCelebrites/project.biow")
                .orElseThrow(() -> new IllegalStateException("Cannot deserialize project"));
        faceMarks = projectHolder.getFaceMarks();
    }

    @PreDestroy
    public void preDestroy() {
        projectHolder.closeProject();
    }

    public List<Result> identify(PwFaceDescriptor descriptor) {
        Comparator<Result> comparing = Comparator.comparing(Result::getQuality);
        return faceMarks.stream().flatMap(mark -> compareWithMark(mark, descriptor))
                .sorted(comparing.reversed())
                .collect(Collectors.toList());
    }

    private Stream<Result> compareWithMark(FaceMark mark, PwFaceDescriptor refDescriptor) {
        Optional<PwFaceDescriptor> descriptor = mark.getPwFaceData().getDescriptor();
        Optional<Subject> subject = projectHolder.getSubject(mark.getSubjectId());

        if (descriptor.isPresent() && subject.isPresent()) {
            float quality = analyserService.compareDescriptors(descriptor.get(), refDescriptor);
            return Stream.of(Result.of(subject.get().getName(), quality));
        } else {
            return Stream.empty();
        }
    }
}
