package greeting.robot;

import com.fasterxml.jackson.databind.ObjectMapper;
import greeting.robot.data.api.Result;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.opencv.core.Mat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RestClient {
    private static final String ENDPOINT_URL = "http://192.168.2.103:9999/uploadFile";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    private static BufferedImage convertToBufferedImage(Mat mat) {
        int type = mat.channels() == 3 ? BufferedImage.TYPE_3BYTE_BGR : BufferedImage.TYPE_BYTE_GRAY;
        BufferedImage image = new BufferedImage(mat.width(), mat.height(), type);
        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        mat.get(0, 0, data);
        return image;
    }

    public List<Result> sendRequest(Mat face) {
        System.out.println("sendRequest");
        long start = System.currentTimeMillis();
        BufferedImage image = convertToBufferedImage(face);

        byte[] imageBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpeg", baos);
            baos.flush();
            imageBytes = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

        HttpPost uploadFile = new HttpPost(ENDPOINT_URL);
        HttpEntity multipart = MultipartEntityBuilder.create()
                .addBinaryBody("file", imageBytes,
                        ContentType.APPLICATION_OCTET_STREAM, "face.jpg").build();

        uploadFile.setEntity(multipart);
        String responseText;
        try {
            CloseableHttpResponse response = httpClient.execute(uploadFile);
            HttpEntity responseEntity = response.getEntity();
            responseText = IOUtils.toString(responseEntity.getContent());
            System.out.println(response.getStatusLine().getStatusCode() + responseText);
            return Arrays.asList(objectMapper.readValue(responseText, Result[].class));
        } catch (IOException e) {
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        System.out.println("sendRequest: " + (end - start));
        return Collections.emptyList();
    }
}
