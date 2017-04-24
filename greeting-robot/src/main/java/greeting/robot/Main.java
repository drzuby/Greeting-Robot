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
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Main {

    private static final String ENDPOINT_URL = "http://localhost:9999/uploadFile";

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;

    private static volatile Mat faceArea;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        VideoCapture camera = new VideoCapture(0);
        camera.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, WIDTH);
        camera.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, HEIGHT);

        Mat colorImg = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3);

        String cascadeFile = "cascades/lbpcascade_frontalface.xml";
        CascadeClassifier cascadeClassifier = new CascadeClassifier(cascadeFile);
        if (cascadeClassifier.empty()) {
            System.err.println("failed to load cascadeClassifier: " + cascadeFile);
            System.exit(1);
        }

        Size minSize = new Size(100, 100);
        Size maxSize = new Size(WIDTH, HEIGHT);

        Window window = new Window(WIDTH, HEIGHT);

        window.exitOnClose();

        new Thread(Main::client).start();

        long t_start, t_read, t_ccls, t_end;
        //noinspection InfiniteLoopStatement
        while (true) {
            t_start = System.currentTimeMillis();

            camera.read(colorImg);

            t_read = System.currentTimeMillis();

            MatOfRect faces = new MatOfRect();
            cascadeClassifier.detectMultiScale(colorImg, faces, 1.1, 3, 0, minSize, maxSize);

            t_ccls = System.currentTimeMillis();

            window.updateImage(colorImg);

            Rect best = null;
            double bestScore = 0;
            for (Rect r : faces.toArray()) {
                Imgproc.rectangle(window.image, r.tl(), r.br(), new Scalar(0, 255, 255), 1);
                double score = getScore(r);
                if (score > bestScore) {
                    best = r;
                    bestScore = score;
                }
            }

            if (best != null) {
                Imgproc.rectangle(window.image, best.tl(), best.br(), new Scalar(0, 255, 0), 1);

                // Add padding
                final double pad = 0.2;
                int width = best.width;
                int height = best.height;

                double padX = width * pad;
                double padY = height * pad;

                Point tl = best.tl();
                Point br = best.br();
                tl.x = Math.max(0, tl.x - padX);
                tl.y = Math.max(0, tl.y - padY);
                br.x = Math.min(colorImg.width(), br.x + padX);
                br.y = Math.min(colorImg.height(), br.y + padY);
                best = new Rect(tl, br);

                Imgproc.rectangle(window.image, best.tl(), best.br(), new Scalar(0, 0, 255), 1);

                // Crop, catch any out of bounds errors

                try {
                    faceArea = colorImg.submat(best);
                } catch (Exception e) {
                    System.err.println(best);
                    e.printStackTrace();
                    continue;
                }

                System.out.println(best);
            }

            t_end = System.currentTimeMillis();

            window.repaint();

            String time = 1000 / (t_end - t_start) + " fps \t[" +
                    "read " + (t_read - t_start) + ", " +
                    "ccls " + (t_ccls - t_read) + ", " +
                    "post " + (t_end - t_ccls) + "]";
            window.setTitle(time);
            System.out.println(time);
        }

    }

    private static BufferedImage convertToBufferedImage(Mat mat) {
        int type = mat.channels() == 3 ? BufferedImage.TYPE_3BYTE_BGR : BufferedImage.TYPE_BYTE_GRAY;
        BufferedImage image = new BufferedImage(mat.width(), mat.height(), type);
        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        mat.get(0, 0, data);
        return image;
    }

    private static double getScore(Rect r) {
        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2;

        int d_hor = WIDTH / 2 - cx;
        int d_ver = HEIGHT / 2 - cy;

        return r.area() / Math.sqrt(d_hor * d_hor + d_ver * d_ver);
    }

    // Http client thread, Face Window display
    private static void client() {
        FaceWindow face_window = new FaceWindow();
        face_window.setTitle("face");

        CloseableHttpClient httpClient = HttpClients.createDefault();

        int counter = 0;

        long t_start, t_end, t_conv;

        while (true) {
            t_start = System.currentTimeMillis();

            Mat face = faceArea;
            if (face == null) {
                try {
                    Thread.sleep(100);
                    continue;
                } catch (InterruptedException e) {
                    break;
                }
            }
            BufferedImage image = convertToBufferedImage(face);

            byte[] imageBytes;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(image, "jpeg", baos);
                baos.flush();
                imageBytes = baos.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            t_conv = System.currentTimeMillis();

            HttpPost uploadFile = new HttpPost(ENDPOINT_URL);

            final String filename = counter++ + ".jpeg";

            HttpEntity multipart = MultipartEntityBuilder.create()
                    .addTextBody("name", filename, ContentType.TEXT_PLAIN)
                    .addBinaryBody("file", imageBytes, ContentType.APPLICATION_OCTET_STREAM, filename)
                    .build();

            uploadFile.setEntity(multipart);
            String responseText = "";
            try {
                CloseableHttpResponse response = httpClient.execute(uploadFile);
                HttpEntity responseEntity = response.getEntity();
                responseText = IOUtils.toString(responseEntity.getContent());
                Result result = objectMapper.readValue(responseText, Result.class);
                System.out.println(result);
            } catch (IOException e) {
                e.printStackTrace();
            }

            t_end = System.currentTimeMillis();

            String time = "[" + (t_end - t_start) + " ms: conv " + (t_conv - t_start) + "]";
            face_window.setTitle(responseText + time);
            face_window.setImage(image);
            face_window.repaint();
        }
    }

}
