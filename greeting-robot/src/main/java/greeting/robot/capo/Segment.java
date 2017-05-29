package greeting.robot.capo;

import java.util.List;

import static java.util.stream.Collectors.joining;

public class Segment {
    public final double startAngle;
    public double endAngle;
    public final double avgDistance;

    public Segment(double startAngle, double endAngle, double avgDistance) {
        this.startAngle = startAngle;
        this.endAngle = endAngle;
        this.avgDistance = avgDistance;
    }

    public double getWidth() {
        return Utils.polarDistance(avgDistance, startAngle, endAngle);
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
