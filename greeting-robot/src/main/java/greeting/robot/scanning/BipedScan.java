package greeting.robot.scanning;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static greeting.robot.scanning.Utils.polarDistance;

public class BipedScan {
    private static final double MIN_LEG_WIDTH = 50;
    private static final double MAX_LEG_WIDTH = 200;

    private static final int MAX_LEG_DISTANCE = 1000;

    private final List<Biped> detectedBipeds = new ArrayList<>();

    private Segment previousLeg;

    private static final boolean ALLOW_OVERLAP = true;

    public BipedScan(Iterable<Segment> scannedEntities) {
        for (Segment e : scannedEntities) {
            update(e);
        }
    }

    public Optional<Biped> getBest(double angle, double distance) {
        return detectedBipeds.stream().min(Comparator.comparing(biped ->
                polarDistance(biped.getDistance(), biped.getAngle(), distance, angle)
        ));
    }

    public void update(Segment e) {
        if (!isLeg(e))
            return;
        if (previousLeg != null && polarDistance(e, previousLeg) <= MAX_LEG_DISTANCE) {
            detectedBipeds.add(new Biped(previousLeg, e));
            //noinspection ConstantConditions
            previousLeg = ALLOW_OVERLAP ? e : null;
        } else {
            previousLeg = e;
        }
    }

    public static Optional<Biped> findBest(List<Segment> segments, double angle, double distance) {
        return new BipedScan(segments).getBest(angle, distance);
    }

    public int size() {
        return detectedBipeds.size();
    }

    private boolean isLeg(Segment e) {
        return MIN_LEG_WIDTH <= e.getWidth() && e.getWidth() <= MAX_LEG_WIDTH;
    }
}
