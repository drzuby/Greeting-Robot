package greeting.robot.data.api;

/**
 * Created by Jakub Janusz on 24.04.2017.
 */
public class Result {
    private String firstName;
    private String lastName;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public String toString() {
        return new StringBuilder("I'm ")
                .append(firstName)
                .append(" ")
                .append(lastName)
                .toString();
    }
}
