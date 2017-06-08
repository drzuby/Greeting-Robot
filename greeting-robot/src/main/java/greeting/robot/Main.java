package greeting.robot;

import greeting.robot.capo.CapoController;
import greeting.robot.capo.SensorLoopMonitorThread;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        CameraController cameraController = new CameraController();
        CapoController capoController = new CapoController("127.0.0.1", 1.0D, cameraController);
        SensorLoopMonitorThread sensorLoopMonitorThread = new SensorLoopMonitorThread(capoController);

        Thread monitorThread = new Thread(sensorLoopMonitorThread);
        capoController.SetMonitoThread(monitorThread);

        new Thread(capoController).start();
        monitorThread.start();

        try {
            cameraController.run();
        } finally {
            capoController.Stop();
            sensorLoopMonitorThread.Stop();
        }
    }

}
