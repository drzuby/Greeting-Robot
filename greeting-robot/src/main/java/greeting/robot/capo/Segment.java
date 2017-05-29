package greeting.robot.capo;

import pl.edu.agh.amber.hokuyo.MapPoint;
import pl.edu.agh.capo.controller.CapoController;

import java.io.IOException;
import java.util.List;

import static java.util.stream.Collectors.joining;

public class Segment {
    public double startAngle;
    public double endAngle;
    public double avgDistance;

    public Segment(double startAngle, double endAngle, double avgDistance) {
        this.startAngle = startAngle;
        this.endAngle = endAngle;
        this.avgDistance = avgDistance;
    }

    public Segment(MapPoint point) {
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
        return "Segment{" +
                "startAngle=" + startAngle +
                ", endAngle=" + endAngle +
                ", avgDistance=" + avgDistance +
                "} width=" + getWidth() + "mm";
    }

    public String asNPArray() {
        return "[" +
                "" + startAngle +
                ", " + endAngle +
                ", " + avgDistance +
                ", " + getWidth() +
                "]";
    }

    public static String asNPArray(List<Segment> scannedEntities) {
        return "[" + scannedEntities.stream()
                .map(Segment::asNPArray)
                .collect(joining(",\n")) + "]";
    }

}
