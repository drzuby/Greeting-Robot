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

    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) throws IOException {
        CascadeClassifier cascadeClassifier = new CascadeClassifier("haarcascade_frontalface_default.xml");
        VideoCapture camera = new VideoCapture(1);
        camera.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, WIDTH);
        camera.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, HEIGHT);
        Mat color = Mat.eye(3, 3, CvType.CV_8UC1);
        Mat gray = Mat.eye(3, 3, CvType.CV_8UC1);

        while (true) {
            long start = System.currentTimeMillis();
            camera.read(color);

            System.out.println("Read: " + (System.currentTimeMillis() - start));
            Imgproc.cvtColor(color, gray, Imgproc.COLOR_RGB2GRAY);

            System.out.println("Cvt: " + (System.currentTimeMillis() - start));

            MatOfRect faces = new MatOfRect();
            cascadeClassifier.detectMultiScale(gray, faces, 1.1, 3, 0, new Size(100, 100), new Size(WIDTH, HEIGHT));

            System.out.println("Haar: " + (System.currentTimeMillis() - start));

            Rect best = null;
            double bestScore = 0;
            for (Rect r : faces.toArray()) {
                double score = getScore(r);
                if (score > bestScore) {
                    best = r;
                    bestScore = score;
                }
            }
            if (best != null) {
                Imgproc.rectangle(color, best.tl(), best.br(), new Scalar(0, 255, 0), 3);

                int padX = best.width;
                best.x -= padX / 2;
                if (best.x < 0) best.x = 0;
                best.width += padX;
                if (best.x + best.width > color.width()) {
                    best.width = color.width() - best.x;
                }

                int padY = best.height;
                best.y -= padY / 2;
                if (best.y < 0) best.y = 0;
                best.height += padY;
                if (best.y + best.height > color.height()) {
                    best.height = color.height() - best.y;
                }

                Imgproc.rectangle(color, best.tl(), best.br(), new Scalar(0, 0, 255), 3);

                BufferedImage colorImage = convertMatToImage(color);

                String colorPath = IMG_OUTPUT_DIR + DATE_FORMAT.format(Calendar.getInstance().getTime()) + ".jpeg";
                writeImage(colorImage, colorPath);

                Mat faceArea;
                try {
                    faceArea = color.submat(best);
                } catch (Exception e) {
                    System.err.println(best);
                    e.printStackTrace();
                    continue;
                }

                BufferedImage faceImage = convertMatToImage(faceArea);

                String facePath = IMG_OUTPUT_DIR + DATE_FORMAT.format(Calendar.getInstance().getTime()) + "-cropped.jpeg";
                writeImage(faceImage, facePath);
                System.out.println("Saving: " + (System.currentTimeMillis() - start));
            }
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

    private static double getScore(Rect r) {
        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2;

        int d_hor = WIDTH / 2 - cx;
        int d_ver = HEIGHT / 2 - cy;

        return r.area() / Math.sqrt(d_hor * d_hor + d_ver * d_ver);
    }
}
