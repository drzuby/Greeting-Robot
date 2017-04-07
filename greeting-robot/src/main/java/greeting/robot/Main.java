package greeting.robot;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import pl.edu.agh.biowiz.face.lib.BitmapImage;
import pl.edu.agh.biowiz.face.lib.pw.PwFaceAnalysisLib;
import pl.edu.agh.biowiz.model.detected.ImageRectangle;
import pl.edu.agh.biowiz.model.detected.PwDetectedFace;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

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
        VideoCapture camera = new VideoCapture(0);
        camera.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, WIDTH);
        camera.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, HEIGHT);

        Mat colorImg = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3);

        String cascadeFile = "cascades/lbpcascade_frontalface.xml";
        CascadeClassifier cascadeClassifier = new CascadeClassifier(cascadeFile);
        if (cascadeClassifier.empty()) {
            System.err.println("failed to load cascadeClassifier: "+cascadeFile);
            System.exit(1);
        }

        Size minSize = new Size(100, 100);
        Size maxSize = new Size(WIDTH, HEIGHT);

        greeting.robot.Window face_window = new greeting.robot.Window(WIDTH, HEIGHT);
        face_window.setTitle("face");
//
        greeting.robot.Window window = new greeting.robot.Window(WIDTH, HEIGHT);
        window.exitOnClose();

        // Face lib
        PwFaceAnalysisLib analyser = new PwFaceAnalysisLib();
        analyser.initialize();

        long t_start, t_read, t_ccls, t_detect, t_desc, t_end;
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
//                Imgproc.rectangle(window.image, r.tl(), r.br(), new Scalar(0, 255, 255), 1);
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
                    faceArea =  colorImg.submat(best);
                } catch (Exception e) {
                    System.err.println(best);
                    e.printStackTrace();
                    continue;
                }

                System.out.println(best);

                face_window.updateImage(faceArea);

                BufferedImage bufferedImage = convertMatToImage(faceArea);

                Optional<PwDetectedFace> pwDetectedFace = analyser.detectFaceInRectangle(bufferedImage,
                        new ImageRectangle(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight()));

                t_detect = System.currentTimeMillis();

                pwDetectedFace.flatMap(face -> {
                    System.out.println(face.toString());
                    return analyser.createDescriptor(face, bufferedImage).getDescriptor();
                })
                        .ifPresent(pwFaceDescriptor -> {
                            System.out.println("quality: "+pwFaceDescriptor.getQuality()+" "+pwFaceDescriptor.getDescriptor());
                        });

                t_desc = System.currentTimeMillis();
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
            } else {
                t_desc=t_detect=System.currentTimeMillis();
            }

            t_end = System.currentTimeMillis();

            window.repaint();
            face_window.repaint();

            String time = 1000 / (t_end - t_start) +" fps \t[" +
                    "read " + (t_read - t_start) + ", " +
                    "ccls " + (t_ccls - t_read) + ", " +
                    "detect" + (t_ccls - t_detect)+ "," +
                    "desc" + (t_detect - t_desc)+ "," +
                    "post " + (t_end - t_ccls) + "]";
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
