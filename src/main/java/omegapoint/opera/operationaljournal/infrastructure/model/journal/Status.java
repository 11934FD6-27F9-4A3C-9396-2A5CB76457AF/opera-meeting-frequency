package omegapoint.opera.operationaljournal.infrastructure.model.journal;


import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;

public class Status {
    public final String value;

    public Status(final String status) {
        notNull(status);
        isTrue(status.length()<200, "length should be less than 200 characters");
        this.value = status;
    }
}
