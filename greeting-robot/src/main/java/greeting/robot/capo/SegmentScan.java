package greeting.robot.capo;

import pl.edu.agh.amber.hokuyo.MapPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class SegmentScan {
    private static final int MINIMUM_DISTANCE_CHANGE = 100;

    private List<Segment> segments = new ArrayList<>();
    private MapPoint prev;
    private double startAngle;
    private double sumDistance;
    private int pointCount;

    public void update(MapPoint p) {
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
                pointCount = 1;
            } else {
                sumDistance += p.getDistance();
                pointCount++;
            }
        }
    }

    public SegmentScan(Iterable<MapPoint> mapPoints) {
        mapPoints.forEach(this::update);
    }

    public List<Segment> getSegments() {
        return segments;
    }

    /* Split into ranges of similar distance */
    public static List<Segment> detectSegments(List<MapPoint> scanPoints) {
        return new SegmentScan(scanPoints).segments;
    }
}
