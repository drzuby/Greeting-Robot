package greeting.robot.capo;

public class Biped {
    private Segment firstLeg;
    private Segment secondLeg;

    public Biped(Segment firstLeg, Segment secondLeg) {
        this.firstLeg = firstLeg;
        this.secondLeg = secondLeg;
    }

    public Segment getFirstLeg() {
        return firstLeg;
    }

    public Segment getSecondLeg() {
        return secondLeg;
    }

    public double getAngle() {
        return Utils.avg(firstLeg.centerAngle(), secondLeg.centerAngle());
    }

    public double getDistance() {
        return Utils.avg(firstLeg.avgDistance, secondLeg.avgDistance);
    }
}
