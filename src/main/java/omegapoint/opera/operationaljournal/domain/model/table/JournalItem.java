package omegapoint.opera.operationaljournal.domain.model.table;

import lombok.EqualsAndHashCode;
import omegapoint.opera.operationaljournal.domain.model.Status;

import java.time.Instant;

import static org.apache.commons.lang3.Validate.notNull;

@EqualsAndHashCode
public final class JournalItem {
    public final String runID;
    public final Integer attempt;
    public final Instant timestamp;
    public final Status status;
    public final String message;
    public final String originFunction;

    public JournalItem(final String runID,
                       final Integer attempt,
                       final Instant timestamp,
                       final Status status,
                       final String message,
                       final String originFunction) {
        if (attempt == null && status != Status.ERROR && status != Status.SUCCESS && status != Status.RESTRICTED ) {
            throw new IllegalArgumentException("Attempt is not allowed to be null. (Exceptions are made for ERROR, RESTRICTED and SUCCESS to make /stop and /restrict more forgiving.)");
        }

        this.runID = notNull(runID);
        this.attempt = attempt;
        this.timestamp = notNull(timestamp);
        this.status = notNull(status);
        this.message = message;
        this.originFunction = originFunction;
    }
}
