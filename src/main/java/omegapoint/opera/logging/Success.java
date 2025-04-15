package omegapoint.opera.logging;

public class Success {

    private static final Success SUCCESS = new Success();

    private Success() {
    }

    public static Success get() {
        return SUCCESS;
    }
}
