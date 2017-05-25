package pl.edu.agh.biowiz.repo.holder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import pl.edu.agh.biowiz.repo.dos.ProjectDo;

import java.io.File;
import java.util.Optional;

import static junit.framework.TestCase.assertTrue;

@SpringBootTest(classes = ProjectSerializer.class)
@RunWith(SpringRunner.class)
public class ProjectSerializerTest {

    @Autowired
    private ProjectSerializer serializer;

    private String location = ".biowiz-data/exampleCelebrites/project.biow";

    @Test
    public void serializerTest() {
        File file = new File(location).getAbsoluteFile();
        assertTrue(file + " exists", file.exists());
        Optional<ProjectDo> read = serializer.read(location);
        assertTrue("read successful", read.isPresent());
    }
}
