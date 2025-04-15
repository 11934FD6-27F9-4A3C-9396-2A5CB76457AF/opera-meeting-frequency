package omegapoint.opera.operationaljournal.infrastructure.model;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.EqualsAndHashCode;
import omegapoint.opera.operationaljournal.domain.model.ConflictId;
import omegapoint.opera.operationaljournal.domain.model.WebhookStep;
import org.apache.commons.lang3.Validate;

import java.util.Optional;
import java.util.UUID;

@EqualsAndHashCode
public class RunItem {
    public final String RunID;
    public final String ParentID;
    public final String OriginTimestamp;
    public final String WebhookStep;
    public final String ConflictID;

    public RunItem(final String runID,
                   final String parentID,
                   final String originTimestamp,
                   final String webhookStep,
                   final String conflictID) {
        this.RunID = runID;
        this.ParentID = parentID;
        this.OriginTimestamp = originTimestamp;
        this.WebhookStep = webhookStep;
        this.ConflictID = conflictID;
    }

    public static Tuple2<RunItem, QueueCheckpoint> fromDomain(omegapoint.opera.operationaljournal.domain.model.table.RunItem domain) {
        return Tuple.of(
                new RunItem(
                        domain.runID.toString(),
                        domain.getParentId().map(UUID::toString).orElse(null),
                        SqlTimestamp.fromInstant(domain.originTimestamp).toString(),
                        domain.step.value,
                        domain.conflictId.id),
                new QueueCheckpoint(
                        domain.runID.toString(),
                        domain.queueCheckpoint.queueName.value,
                        domain.queueCheckpoint.blobReference.containerName,
                        domain.queueCheckpoint.blobReference.path)
        );
    }

    public omegapoint.opera.operationaljournal.domain.model.table.RunItem toDomain(QueueCheckpoint queueCheckpoint) {

        Validate.isTrue(queueCheckpoint.RunID.equals(this.RunID));

        return new omegapoint.opera.operationaljournal.domain.model.table.RunItem(
                UUID.fromString(this.RunID),
                Optional.ofNullable(this.ParentID).map(UUID::fromString).orElse(null),
                SqlTimestamp.parse(this.OriginTimestamp).toInstant(),
                new WebhookStep(this.WebhookStep),
                new ConflictId(this.ConflictID),
                queueCheckpoint.toDomain());
    }
}
