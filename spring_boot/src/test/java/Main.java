import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class Main {
    private static String FILENAME = "exampleFile.jpeg";

    public static void main(String[] args) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost uploadFile = new HttpPost("http://localhost:9999/uploadFile");
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addTextBody("name", "someFile.jpeg", ContentType.TEXT_PLAIN);

        BufferedImage bufferedImage = ImageIO.read(new File(FILENAME));
        byte[] imageBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, "jpeg", baos);
            baos.flush();
            imageBytes = baos.toByteArray();
        }

        builder.addBinaryBody(
                "file",
                imageBytes,
                ContentType.APPLICATION_OCTET_STREAM,
                FILENAME);

        HttpEntity multipart = builder.build();
        uploadFile.setEntity(multipart);
        CloseableHttpResponse response = httpClient.execute(uploadFile);
        HttpEntity responseEntity = response.getEntity();
        System.out.println(IOUtils.toString(responseEntity.getContent()));
    }
}
