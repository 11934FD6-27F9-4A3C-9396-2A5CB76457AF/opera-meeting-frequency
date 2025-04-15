package omegapoint.opera.operationaljournal.api.model.request;

import lombok.EqualsAndHashCode;
import omegapoint.opera.operationaljournal.domain.model.ConflictId;
import omegapoint.opera.operationaljournal.domain.model.StartItem;
import omegapoint.opera.operationaljournal.domain.model.Status;
import omegapoint.opera.operationaljournal.domain.model.WebhookStep;
import omegapoint.opera.operationaljournal.domain.model.table.JournalItem;
import omegapoint.opera.operationaljournal.domain.model.table.RunItem;

import java.util.UUID;

import static org.apache.commons.lang3.Validate.notNull;

@EqualsAndHashCode
public final class StartRequest implements StartItem {

    public final String runId;
    public final StartBody body;

    //NOTE: The absence of JSON-annotations is intentional. This class is not a JSON model.
    public StartRequest(final String runId,
                        final StartBody body) {
        this.runId = notNull(runId);
        this.body = notNull(body);
    }

    @Override
    public JournalItem toJournalItem() {
        return new JournalItem(
                runId,
                1,
                body.runStartTime.toInstant(),
                Status.STARTED,
                body.message,
                body.originFunction);
    }

    @Override
    public RunItem toRunItem() {
        return new RunItem(
                UUID.fromString(runId),
                body.parentRunId,
                body.originTimestamp.toInstant(),
                body.checkpoint.toDomain().checkpointType,
                body.checkpoint.toDomain().checkpointPath,
                body.blobReference,
                new ConflictId(body.conflictId),
                new WebhookStep(body.webhookStep.value)
        );
    }
}
