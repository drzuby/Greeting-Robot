package greeting.robot;

import greeting.robot.data.api.Result;
import org.opencv.core.*;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.util.Optional;

public class CameraController implements Runnable {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private static final int HEIGHT = 1080;
    private static final int WIDTH = 1920;

    private static final Size MAX_SIZE = new Size(WIDTH, HEIGHT);
    private static final Size MIN_SIZE = new Size(100, 100);

    private final RestClient restClient = new RestClient();
    private CascadeClassifier cascadeClassifier;

    private Mat colorImg;

    private VideoCapture camera;

    @Override
    public void run() {
        init();

        while (true) {
            singleScan().map(restClient::sendRequest)
                    .ifPresent(results -> {
                        System.out.println("\n\n\n\n\n\n");
                        results.forEach(this::say);
                        System.out.println("\n\n\n\n\n\n");
                    });
            try {
                sleep();
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public synchronized void wakeUp() {
        this.notify();
    }

    private synchronized void sleep() throws InterruptedException {
//        System.out.println("sleeping");
        wait();
//        System.out.println("waking up");
    }

    private void init() {
        camera = new VideoCapture(0);
        camera.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, WIDTH);
        camera.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, HEIGHT);
        colorImg = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3);
        String cascadeFile = "cascades/lbpcascade_frontalface.xml";
        cascadeClassifier = new CascadeClassifier(cascadeFile);
        if (cascadeClassifier.empty()) {
            throw new RuntimeException("failed to load cascadeClassifier: " + cascadeFile);
        }
    }

    private void say(Result result) {
        System.out.println(result);
    }

    private Optional<Mat> singleScan() {
        long t_start = System.currentTimeMillis();
        camera.read(colorImg);
        MatOfRect faces = new MatOfRect();
        cascadeClassifier.detectMultiScale(colorImg, faces, 1.1, 3, 0, MIN_SIZE, MAX_SIZE);
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
            // Add padding
            final double pad = 0.2;
            int width = best.width;
            int height = best.height;

            double padX = width * pad;
            double padY = height * pad;

            // Crop, catch any out of bounds errors
            Point tl = best.tl();
            Point br = best.br();
            tl.x = Math.max(0, tl.x - padX);
            tl.y = Math.max(0, tl.y - padY);
            br.x = Math.min(colorImg.width(), br.x + padX);
            br.y = Math.min(colorImg.height(), br.y + padY);
            best = new Rect(tl, br);
        }
        long t_end = System.currentTimeMillis();
//        System.out.println("singleScan: " + (t_end - t_start) + "ms");
        return Optional.ofNullable(best).map(colorImg::submat);
    }

    private static double getScore(Rect r) {
        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2;

        int d_hor = WIDTH / 2 - cx;
        int d_ver = HEIGHT / 2 - cy;

        return r.area() / Math.sqrt(d_hor * d_hor + d_ver * d_ver);
    }
}
