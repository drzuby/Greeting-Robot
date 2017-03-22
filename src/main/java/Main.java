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
import java.util.Date;

/**
 * Created by drzuby on 13.03.17.
 */
public class Main {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss,SSS");
    private static final String IMG_OUTPUT_DIR = "img/";

    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;

    public static void main(String[] args) throws IOException {
        VideoCapture camera = new VideoCapture(1);
        camera.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, WIDTH);
        camera.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, HEIGHT);

        Mat colorImg = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3);
        Mat grayImg = new Mat(HEIGHT, WIDTH, CvType.CV_8UC1);

        CascadeClassifier cascadeClassifier = new CascadeClassifier("haarcascade_frontalface_default.xml");

        Size minSize = new Size(100, 100);
        Size maxSize = new Size(WIDTH, HEIGHT);

        Window window = new Window(WIDTH, HEIGHT);

        long t_start, t_read, t_conv, t_haar, t_end;
        while (true) {
            t_start = System.currentTimeMillis();

            camera.read(colorImg);

            t_read = System.currentTimeMillis();

            Imgproc.cvtColor(colorImg, grayImg, Imgproc.COLOR_BGR2GRAY);

            t_conv = System.currentTimeMillis();

            MatOfRect faces = new MatOfRect();
            cascadeClassifier.detectMultiScale(grayImg, faces, 1.1, 3, 0, minSize, maxSize);

            t_haar = System.currentTimeMillis();

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
                int width = best.width;
                best.x = Math.max(0, best.x - width/2);
                best.width = Math.min(2*width, colorImg.width() - best.x);

                int height = best.height;
                best.y = Math.max(0, best.y - height/2);
                best.height = Math.min(2*height, colorImg.height() - best.y);

                Imgproc.rectangle(window.image, best.tl(), best.br(), new Scalar(0, 0, 255), 1);

                // Crop, catch any out of bounds errors
                Mat faceArea;
                try {
                    faceArea = colorImg.submat(best);
                } catch (Exception e) {
                    System.err.println(best);
                    e.printStackTrace();
                    continue;
                }

                String timestamp = DATE_FORMAT.format(new Date());
//                // Save full image
//                BufferedImage colorImage = convertMatToImage(colorImg);
//                String colorPath = IMG_OUTPUT_DIR + timestamp + ".jpeg";
//                writeImage(colorImage, colorPath);
//
//                // Save cropped face
//                BufferedImage faceImage = convertMatToImage(faceArea);
//                String facePath = IMG_OUTPUT_DIR + timestamp + "-cropped.jpeg";
//                writeImage(faceImage, facePath);
            }

            t_end = System.currentTimeMillis();

            window.repaint();

            String time = (t_end - t_start) + "ms \t[" +
                    "read " + (t_read - t_start) + ", " +
                    "conv " + (t_conv - t_read) + ", " +
                    "haar " + (t_haar - t_conv) + ", " +
                    "post " + (t_end - t_haar) + "]";
            window.setTitle(time);
            System.out.println(time);
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
