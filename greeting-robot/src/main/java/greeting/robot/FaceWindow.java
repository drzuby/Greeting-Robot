package greeting.robot;

import javax.swing.*;
import java.awt.*;

class FaceWindow extends JFrame {
    private final ImageIcon icon;

    FaceWindow() throws HeadlessException {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        icon = new ImageIcon();
        add(new JLabel(icon), constraints);

        pack();
        setVisible(true);
    }

    void setImage(Image image) {
        icon.setImage(image);
        pack();
    }
}
