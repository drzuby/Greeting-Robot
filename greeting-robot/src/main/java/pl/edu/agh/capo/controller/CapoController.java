package pl.edu.agh.capo.controller;

import greeting.robot.capo.Biped;
import greeting.robot.capo.BipedScan;
import greeting.robot.capo.Segment;
import greeting.robot.capo.SegmentScan;
import pl.edu.agh.amber.common.AmberClient;
import pl.edu.agh.amber.hokuyo.HokuyoProxy;
import pl.edu.agh.amber.hokuyo.MapPoint;
import pl.edu.agh.amber.hokuyo.Scan;
import pl.edu.agh.amber.roboclaw.RoboclawProxy;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CapoController
        implements Runnable {

    private double maxVelocity = 1.0D;
    private double currentVelocityLeft;
    private double currentVelocityRight;

    private RoboclawProxy roboclawProxy;
    private HokuyoProxy hokuyoProxy;
    private Thread monitorThread;
    private boolean isRun = true;

    public static final double TARGET_DISTANCE = 700;

    public CapoController(String robotIP, double maxVelocity)
            throws IOException {
        if (maxVelocity < 2 && maxVelocity > 0)
            this.maxVelocity = maxVelocity;

        AmberClient client = new AmberClient(robotIP, 26233);
        this.roboclawProxy = new RoboclawProxy(client, 0);
        this.hokuyoProxy = new HokuyoProxy(client, 0);
    }

    public void SetMonitoThread(Thread monitorThread) {
        this.monitorThread = monitorThread;
    }

    public void Stop() {
        this.isRun = false;
        SetCapoVelocity(0.0D, 0.0D);
    }

    /**
     * Controller thread - main control loop here
     */
    public void run() {
        while (this.isRun) {
            List<MapPoint> scanPoints = getScan();
            if (scanPoints.isEmpty()) continue;

            // TODO: replace with wandering
            this.monitorThread.interrupt();
            SetCapoVelocity(0, 0);

            try {
                // FIXME: DEBUG only, remove later
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                continue;
            }
            // END

            List<Segment> scannedEntities = SegmentScan.detectSegments(scanPoints);
            Optional<Biped> best = BipedScan.findBest(scannedEntities);

            if (best.isPresent()) {
                System.out.println("Target at " + best.get().getAngle());
                // 	Assume human, go towards it (old capo code)
            } else {
                System.out.println("No targets in sight");
            }
        }

    }

    private List<MapPoint> getScan() {
        Scan scan;
        try {
            scan = this.hokuyoProxy.getSingleScan();
        } catch (IOException e) {
            SetCapoVelocity(0.0D, 0.0D);
            System.out.println("FATAL Exception in hokuyoProxy.getSingleScan(): " + e.getMessage());
            Stop();
            return Collections.emptyList();
        }
        List<MapPoint> scanPoints;
        try {
            scanPoints = scan.getPoints(5000);
            if (scanPoints == null) System.out.println("getPoints() timed out");
        } catch (Exception e) {
            SetCapoVelocity(0.0D, 0.0D);
            System.out.println("Exception in scan.getPoints: " + e.getMessage());
            return Collections.emptyList();
        }
        return scanPoints;
    }

    /**
     * Sets the velocity of the robot
     *
     * @param vLeft  - left side velocity in m/s
     * @param vRight - right side velocity in m/s
     */
    private synchronized void SetCapoVelocity(double vLeft, double vRight) {
        if (vLeft > maxVelocity) vLeft = maxVelocity;
        if (-vLeft > maxVelocity) vLeft = -maxVelocity;
        if (vRight > maxVelocity) vRight = maxVelocity;
        if (-vRight > maxVelocity) vRight = -maxVelocity;
        this.currentVelocityLeft = vLeft;
        this.currentVelocityRight = vRight;
        System.out.println("At: " + System.currentTimeMillis() + " set velocity from thread " + Thread.currentThread().getId() + ": left=" + vLeft + "; right=" + vRight);
        try {
            this.roboclawProxy.sendMotorsCommand((int) (vLeft * 1000.0D), (int) (vRight * 1000.0D), (int) (vLeft * 1000.0D), (int) (vRight * 1000.0D));
        } catch (Exception e) {
            System.out.println("Exception in roboclawProxy.sendMotorsCommand: " + e.getMessage());
        }
    }


    /**
     * Reduces the velocity - divides it by 2.
     * Called by the sensor monitoring thread when reading is late
     */
    void reduceSpeedDueToSensorReadingTimeout() {
        System.out.print("-> reduceSpeedDueToSensorReadingTimeout ");
        SetCapoVelocity(this.currentVelocityLeft / 2.0D, this.currentVelocityRight / 2.0D);
    }
}
