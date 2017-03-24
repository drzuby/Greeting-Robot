import org.opencv.core.CvType;
import org.opencv.core.Mat;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;

public class Window extends JFrame {

    final Mat image;
    private final byte[] data;

    public Window(int width, int height) throws HeadlessException {
        image = new Mat(height, width, CvType.CV_8UC3);

        BufferedImage bufferedImage = new BufferedImage(width, height, TYPE_3BYTE_BGR);
        data = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();

        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        add(new JLabel(new ImageIcon(bufferedImage)), constraints);

        pack();
        setVisible(true);
    }

    public void exitOnClose() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public void updateImage(Mat mat) {
        mat.copyTo(image.submat(0,mat.rows(),0,mat.cols()));
    }

    @Override
    public void repaint() {
        image.get(0,0,data);
        super.repaint();
    }
}
