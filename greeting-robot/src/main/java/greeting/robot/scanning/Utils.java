package greeting.robot.scanning;

final class Utils {
    private Utils() {
    }


    static double polarDistance(double r1, double a1, double r2, double a2) {
        return Math.sqrt(r1 * r1 + r2 * r2 - 2 * r1 * r2 * Math.cos(Math.toRadians(a1 - a2)));
    }

    static double polarDistance(Segment e1, Segment e2) {
        return polarDistance(e1.avgDistance, e1.centerAngle(), e2.avgDistance, e2.centerAngle());
    }

    static double polarDistance(double r, double a1, double a2) {
        return r * Math.sqrt(2 * (1 - Math.cos(Math.toRadians(a1 - a2))));
    }

    static double avg(double a, double b) {
        return (a + b) / 2;
    }
}
