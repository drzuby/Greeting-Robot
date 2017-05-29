package greeting.robot.capo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static greeting.robot.capo.Utils.polarDistance;

public class BipedScan {
    private static final double MIN_LEG_WIDTH = 50;
    private static final double MAX_LEG_WIDTH = 200;

    private static final int MAX_LEG_DISTANCE = 1000;

    private final List<Biped> detectedBipeds = new ArrayList<>();

    private Segment previousLeg;

    public BipedScan(Iterable<Segment> scannedEntities) {
        for (Segment e : scannedEntities) {
            update(e);
        }
    }

    public Optional<Biped> getBest() {
        return detectedBipeds.stream().min(Comparator.comparing(BipedScan::cost));
    }

    private static double cost(Biped biped) {
        return biped.getDistance();
    }

    public void update(Segment e) {
        if (!isLeg(e)) return;
        if (previousLeg != null && polarDistance(e, previousLeg) <= MAX_LEG_DISTANCE) {
            detectedBipeds.add(new Biped(previousLeg, e));
            previousLeg = null; // or e, if overlapping is ok
        } else {
            previousLeg = e;
        }
    }

    public static Optional<Biped> findBest(List<Segment> segments) {
        return new BipedScan(segments).getBest();
    }

    private boolean isLeg(Segment e) {
        return MIN_LEG_WIDTH <= e.getWidth() && e.getWidth() <= MAX_LEG_WIDTH;
    }
}
