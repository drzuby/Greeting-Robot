package pl.edu.agh.capo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.edu.agh.amber.common.AmberClient;
import pl.edu.agh.amber.hokuyo.HokuyoProxy;
import pl.edu.agh.amber.hokuyo.MapPoint;
import pl.edu.agh.amber.hokuyo.Scan;
import pl.edu.agh.amber.roboclaw.RoboclawProxy;

import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.joining;

public class CapoController
        implements Runnable {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    protected double maxVelocity = 0.2D;
    protected double currentVelocityLeft;
    protected double currentVelocityRight;
    protected AmberClient client;

    protected RoboclawProxy roboclawProxy;
    protected HokuyoProxy hokuyoProxy;
    protected Thread monitorThread;
    protected boolean isRun = true;

    private static final int MINIMUM_DISTANCE_CHANGE = 100;

    private static final double TARGET_DISTANCE = 700;

    private static final double MAX_FOV = 45;
    private static final double MIN_FOV = 15;

    public static final double MIN_LEG_WIDTH = 50;
    public static final double MAX_LEG_WIDTH = 200;

    public CapoController(String robotIP, double maxVelocity)
            throws IOException {
        if (maxVelocity < 2 && maxVelocity > 0)
            this.maxVelocity = maxVelocity;

        this.client = new AmberClient(robotIP, 26233);
        this.roboclawProxy = new RoboclawProxy(this.client, 0);
        this.hokuyoProxy = new HokuyoProxy(this.client, 0);
    }

    public static double polarDistance(double r1, double theta1, double r2, double theta2) {
        return Math.sqrt(r1 * r1 + r2 * r2 - 2 * r1 * r2 * Math.cos(Math.toRadians(theta1 - theta2)));
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

        double t0 = System.currentTimeMillis();
        double deltaTime, t1;
        double fov = MAX_FOV;
        double targetAngle = 0;

        while (this.isRun) {
            t1 = System.currentTimeMillis();
            deltaTime = (t1 - t0) / 1000;
            t0 = t1;

            Scan scan;
            try {
                scan = this.hokuyoProxy.getSingleScan();
            } catch (IOException e) {

                SetCapoVelocity(0.0D, 0.0D);
                System.out.println("FATAL Exception in hokuyoProxy.getSingleScan(): " + e.getMessage());
                return;
            }
            List<MapPoint> scanPoints;
            try {
                scanPoints = scan.getPoints();
            } catch (Exception e) {
                SetCapoVelocity(0.0D, 0.0D);
                System.out.println("Exception in scan.getPoints: " + e.getMessage());
                continue;
            }
//            this.monitorThread.interrupt();
//            SetCapoVelocity(0, 0);
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                continue;
//            }

            List<ScannedEntity> scannedEntities = detectEntities(scanPoints, targetAngle, fov);

            /* Debug: print all ranges */
//			System.out.println(ScannedEntity.asNPArray(scannedEntities));
//            System.out.println(scannedEntities.stream()
//                    .map(String::valueOf).collect(joining("\n")));

            ScannedEntity firstLeg = null, secondLeg = null;
            for (ScannedEntity e : scannedEntities) {
                double width = e.getWidth();

                if (width < MIN_LEG_WIDTH || MAX_LEG_WIDTH < width) {
                    continue;
                }

                if (firstLeg == null) {
                    firstLeg = e;
                } else {
                    double distance = polarDistance(
                            firstLeg.avgDistance, firstLeg.centerAngle(),
                            e.avgDistance, e.centerAngle()
                    );
                    if (distance <= 1000) {
                        secondLeg = e;
                        break;
                    } else {
                        firstLeg = e;
                    }
                }
            }

            if (secondLeg != null) {
//            System.out.println("Legs:\n" +
//                    firstLeg + "\n" +
//                    secondLeg);

                targetAngle = (firstLeg.centerAngle() + secondLeg.centerAngle()) / 2;
                double targetDistance = (firstLeg.avgDistance + secondLeg.avgDistance) / 2;

                System.out.println("Target at " + targetAngle + ", " + targetDistance);

                double deltaDistance = targetDistance - TARGET_DISTANCE;
                double forwardVelocity = deltaDistance * 0;
                double turn = targetAngle / 100;
                SetCapoVelocity(forwardVelocity + turn, forwardVelocity - turn);
            } else {
                System.out.println("Did not find a pair of legs");
                SetCapoVelocity(-0.05, 0.05);
            }
            this.monitorThread.interrupt();

            System.out.println(deltaTime);
            // TODO: scannedEntities:
            // 	filter by width [human leg],

            // 	find 2 points close enough to each other,
            // 	Assume human, go towards it (old capo code)
        }

    }

    /* Split into ranges of similar distance */
    private List<ScannedEntity> detectEntities(List<MapPoint> scanPoints, double target, double fov) {
        if (scanPoints.isEmpty()) return Collections.emptyList();
        List<ScannedEntity> scannedEntities = new ArrayList<>();

        Iterator<MapPoint> iterator = scanPoints.iterator();
        MapPoint prev;
        do {
            prev = iterator.next();
        } while (Math.abs(prev.getAngle() - target) > fov);

        ScannedEntity current = new ScannedEntity(prev);
        int n = 1;
        double dR;
        MapPoint p = prev;
        while (iterator.hasNext()) {
            p = iterator.next();
            if (Math.abs(p.getAngle() - target) > fov) break;
            dR = (p.getDistance() - prev.getDistance()) /
                    (p.getAngle() - prev.getAngle());
            if (Math.abs(dR) > MINIMUM_DISTANCE_CHANGE) {
                current.endAngle = p.getAngle();
                current.avgDistance /= n;

                if (current.getWidth() > 1) scannedEntities.add(current);

                current = new ScannedEntity(p);
                n = 1;
            } else {
                current.avgDistance += p.getDistance();
                n++;
            }
            prev = p;
        }
        current.endAngle = p.getAngle();
        current.avgDistance /= n;
        if (current.getWidth() > 1) scannedEntities.add(current);
        return scannedEntities;
    }

    private static class ScannedEntity {
        public double startAngle;
        public double endAngle;
        public double avgDistance;

        public ScannedEntity(MapPoint point) {
            this.startAngle = point.getAngle();
            this.avgDistance = point.getDistance();
        }

        public double getWidth() {
            return avgDistance * Math.sqrt(2 * (1 - Math.cos(Math.toRadians(startAngle - endAngle))));
        }

        public double centerAngle() {
            return (startAngle + endAngle) / 2;
        }

        @Override
        public String toString() {
            try {
                return OBJECT_MAPPER.writeValueAsString(this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public String asNPArray() {
            return "[" +
                    "" + startAngle +
                    ", " + endAngle +
                    ", " + avgDistance +
                    ", " + getWidth() +
                    "]";
        }

        public static String asNPArray(List<ScannedEntity> scannedEntities) {
            return "[" + scannedEntities.stream()
                    .map(ScannedEntity::asNPArray)
                    .collect(joining(",\n")) + "]";
        }

    }

    /**
     * Sets the velocity of the robot
     *
     * @param vLeft  - left side velocity in m/s
     * @param vRight - right side velocity in m/s
     */
    protected synchronized void SetCapoVelocity(double vLeft, double vRight) {
        if (vLeft > maxVelocity) vLeft = maxVelocity;
        if (-vLeft > maxVelocity) vLeft = -maxVelocity;
        if (vRight > maxVelocity) vRight = maxVelocity;
        if (-vRight > maxVelocity) vRight = -maxVelocity;
        this.currentVelocityLeft = vLeft;
        this.currentVelocityRight = vRight;
        System.out.println("At: " + System.currentTimeMillis() + " set velocity from tread " + Thread.currentThread().getId() + ": left=" + vLeft + "; right=" + vRight);
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
    public void reduceSpeedDueToSensorRedingTimeout() {
        System.out.print("-> reduceSpeedDueToSensorRedingTimeout");
        SetCapoVelocity(this.currentVelocityLeft / 2.0D, this.currentVelocityRight / 2.0D);
    }
}
