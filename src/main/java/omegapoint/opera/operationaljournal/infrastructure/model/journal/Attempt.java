package omegapoint.opera.operationaljournal.infrastructure.model.journal;

import static org.apache.commons.lang3.Validate.*;


public class Attempt {
    public final Integer value;

    public Attempt(final Integer attempt) {
        notNull(attempt, "Attempt must not be null");
        isTrue(attempt == 6,"Attempt must be 6");
        this.value = attempt;
    }
}
