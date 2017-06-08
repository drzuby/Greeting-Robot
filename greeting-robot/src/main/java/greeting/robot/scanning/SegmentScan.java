package greeting.robot.scanning;

import pl.edu.agh.amber.hokuyo.MapPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SegmentScan {
    private static final int MINIMUM_DISTANCE_CHANGE = 100;

    private static final int LOWER_BOUND = 150;
    private static final int UPPER_BOUND = 5000;

    private final List<Segment> segments = new ArrayList<>();
    private MapPoint prev;
    private double startAngle;
    private double sumDistance;
    private int pointCount;

    private void update(MapPoint p) {
        if (p.getDistance() < LOWER_BOUND || p.getDistance() > UPPER_BOUND)
            return;
        if (prev == null) {
            prev = p;
            pointCount = 1;
            startAngle = p.getAngle();
            sumDistance += p.getDistance();
        } else {
            double dR = (p.getDistance() - prev.getDistance()) / (p.getAngle() - prev.getAngle());
            if (Math.abs(dR) > MINIMUM_DISTANCE_CHANGE) {
                segments.add(new Segment(startAngle, p.getAngle(), sumDistance / pointCount));
                sumDistance = p.getDistance();
                startAngle = p.getAngle();
                pointCount = 1;
            } else {
                sumDistance += p.getDistance();
                pointCount++;
            }
            prev = p;
        }
    }

    public SegmentScan(Collection<MapPoint> mapPoints) {
        mapPoints.forEach(this::update);
    }

    /* Split into ranges of similar distance */
    public static List<Segment> detectSegments(List<MapPoint> scanPoints) {
        return new SegmentScan(scanPoints).segments;
    }
}
