package greeting.robot.capo;

public final class Utils {
    private Utils() {
    }


    public static double polarDistance(double r1, double theta1, double r2, double theta2) {
        return Math.sqrt(r1 * r1 + r2 * r2 - 2 * r1 * r2 * Math.cos(Math.toRadians(theta1 - theta2)));
    }

    public static double polarDistance(Segment e1, Segment e2) {
        return polarDistance(e1.avgDistance, e1.centerAngle(), e2.avgDistance, e2.centerAngle());
    }

    public static double avg(double a, double b) {
        return (a+b)/2;
    }
}
