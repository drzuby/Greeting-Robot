import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by drzuby on 13.03.17.
 */
public class Main {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss,SSS");
    private static final String IMG_OUTPUT_DIR = "img/";

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) throws IOException {
        CascadeClassifier cascadeClassifier = new CascadeClassifier("haarcascade_frontalface_default.xml");
        VideoCapture camera = new VideoCapture(1);
        camera.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, 1920);
        camera.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, 1080);
        Mat color = Mat.eye(3, 3, CvType.CV_8UC1);
        Mat gray = Mat.eye(3, 3, CvType.CV_8UC1);

        while (true) {
            long start = System.currentTimeMillis();
            camera.read(color);

            System.out.println("Read: " + (System.currentTimeMillis() - start));
            Imgproc.cvtColor(color, gray, Imgproc.COLOR_RGB2GRAY);

            System.out.println("Cvt: " + (System.currentTimeMillis() - start));

            MatOfRect faces = new MatOfRect();
            cascadeClassifier.detectMultiScale(gray, faces);

            System.out.println("Haar: " + (System.currentTimeMillis() - start));

            for (Rect r : faces.toArray()) {
                Imgproc.rectangle(color, r.tl(), r.br(), new Scalar(0, 255, 0), 3);
            }

            BufferedImage colorImage = convertMatToImage(color);
            BufferedImage grayImage = convertMatToImage(gray);
            String colorPath = IMG_OUTPUT_DIR + DATE_FORMAT.format(Calendar.getInstance().getTime()) + ".jpeg";
            String grayPath = IMG_OUTPUT_DIR + DATE_FORMAT.format(Calendar.getInstance().getTime()) + "-gray.jpeg";
            writeImage(colorImage, colorPath);
            writeImage(grayImage, grayPath);

            System.out.println("Saving: " + (System.currentTimeMillis() - start));

            System.out.println("----------");
        }

    }

    private static void writeImage(BufferedImage image, String path) throws IOException {
        ImageIO.write(image, "jpeg", new File(path));
    }

    private static BufferedImage convertMatToImage(Mat mat) {
        int type = mat.channels() == 3 ? BufferedImage.TYPE_3BYTE_BGR : BufferedImage.TYPE_BYTE_GRAY;
        BufferedImage image = new BufferedImage(mat.width(), mat.height(), type);
        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        mat.get(0, 0, data);
        return image;
    }
}
