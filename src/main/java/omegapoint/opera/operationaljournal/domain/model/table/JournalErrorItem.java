package omegapoint.opera.operationaljournal.domain.model.table;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class JournalErrorItem {
    public final String runID;
    public final Integer attempt;
    public final String webhookStep;
    public final String timestamp;
    public final String status;
    public final String message;
    public final String originFunction;
    public final String parentID;

    public JournalErrorItem(final String runID,
                            final Integer attempt,
                            final String timeStamp,
                            final String status,
                            final String message,
                            final String originFunction,
                            final String webhookStep,
                            final String parentID) {
        this.runID = runID;
        this.attempt = attempt;
        this.timestamp = timeStamp;
        this.status = status;
        this.message = message;
        this.originFunction = originFunction;
        this.webhookStep = webhookStep;
        this.parentID = parentID;
    }
}
