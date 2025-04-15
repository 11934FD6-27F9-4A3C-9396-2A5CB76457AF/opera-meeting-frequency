package omegapoint.opera.operationaljournal.infrastructure.model.journal;


import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;

public class RunId {
    public final String value;

    public RunId(final String runId) {
        notNull(runId);
        isTrue(runId.length()<200, "length should be less than 200 characters");
        this.value = notNull(runId);
    }
}
