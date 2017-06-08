package greeting.robot.capo;

public class Biped {
    private final Segment firstLeg;
    private final Segment secondLeg;

    public Biped(Segment firstLeg, Segment secondLeg) {
        this.firstLeg = firstLeg;
        this.secondLeg = secondLeg;
    }

    public double getAngle() {
        return Utils.avg(firstLeg.centerAngle(), secondLeg.centerAngle());
    }

    public double getDistance() {
        return Utils.avg(firstLeg.avgDistance, secondLeg.avgDistance);
    }
}
