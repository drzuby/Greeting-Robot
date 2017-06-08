package greeting.robot.data.api;

/**
 * Created by Jakub Janusz on 24.04.2017.
 */
public class Result implements Comparable<Result> {
    private String name;
    private float quality;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getQuality() {
        return quality;
    }

    public void setQuality(float quality) {
        this.quality = quality;
    }

    @Override
    public String toString() {
        return name + ", quality: " + quality;
    }

    public static Result of(String name, float quality) {
        Result result = new Result();
        result.name = name;
        result.quality = quality;
        return result;
    }

    @Override
    public int compareTo(Result o) {
        return Float.compare(this.quality, o.getQuality());
    }
}
