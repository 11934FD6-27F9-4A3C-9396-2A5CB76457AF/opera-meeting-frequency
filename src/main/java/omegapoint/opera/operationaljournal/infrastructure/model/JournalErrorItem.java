package omegapoint.opera.operationaljournal.infrastructure.model;

import lombok.EqualsAndHashCode;
import omegapoint.opera.operationaljournal.api.model.response.Run;
import omegapoint.opera.operationaljournal.infrastructure.model.journal.*;
@EqualsAndHashCode
public class JournalErrorItem {

    public final RunId runID;
    public final Attempt attempt;
    public final WebhookStep webhookStep;
    public final TimeStamp timestamp;
    public final Status status;
    public final Message message;
    public final OriginFunction originFunction;
    public final RunId parentID;

    public JournalErrorItem(final RunId runID,
                            final Attempt attempt,
                            final TimeStamp timeStamp,
                            final Status status,
                            final Message message,
                            final OriginFunction originFunction,
                            final WebhookStep webhookStep, 
                            final RunId parentID) {
        this.runID = runID;
        this.attempt = attempt;
        this.timestamp = timeStamp;
        this.status = status;
        this.message = message;
        this.originFunction = originFunction;
        this.webhookStep = webhookStep;
        this.parentID=parentID;
    }
    public omegapoint.opera.operationaljournal.domain.model.table.JournalErrorItem toDomain(){
        return new omegapoint.opera.operationaljournal.domain.model.table.JournalErrorItem(
                runID.value,
                attempt.value,
                timestamp.value,
                status.value,
                message.value,
                originFunction.value,
                webhookStep.value,
                parentID.value
        );
    }



}
