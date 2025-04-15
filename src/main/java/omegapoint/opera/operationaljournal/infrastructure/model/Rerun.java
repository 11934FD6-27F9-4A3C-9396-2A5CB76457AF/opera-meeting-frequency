package omegapoint.opera.operationaljournal.infrastructure.model;

import lombok.EqualsAndHashCode;
import omegapoint.opera.operationaljournal.domain.model.BlobReference;
import omegapoint.opera.operationaljournal.domain.model.WebhookStep;

import java.util.UUID;

@EqualsAndHashCode
public class Rerun {
    public final String RunID;
    public final int Attempt;
    public final String WebhookStep;
    public final String OriginTimestamp;
    public final String QueueName;
    public final String BlobContainerName;
    public final String BlobPath;

    public Rerun(final String runId,
                 final int attempt,
                 final String webhookStep,
                 final String originTimestamp,
                 final String queueName,
                 final String blobContainerName,
                 final String blobPath) {
        this.RunID = runId;
        this.Attempt = attempt;
        this.WebhookStep = webhookStep;
        this.OriginTimestamp = originTimestamp;
        this.QueueName = queueName;
        this.BlobContainerName = blobContainerName;
        this.BlobPath = blobPath;
    }

    public omegapoint.opera.operationaljournal.domain.model.table.Rerun toDomain() {
        return new omegapoint.opera.operationaljournal.domain.model.table.Rerun(
                UUID.fromString(RunID),
                Attempt,
                new WebhookStep(WebhookStep),
                SqlTimestamp.parse(OriginTimestamp).toUTCZonedDateTime(),
                QueueName,
                new BlobReference(BlobContainerName, BlobPath));
    }
}
