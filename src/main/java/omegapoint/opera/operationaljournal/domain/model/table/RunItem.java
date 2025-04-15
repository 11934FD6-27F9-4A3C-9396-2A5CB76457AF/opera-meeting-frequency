package omegapoint.opera.operationaljournal.domain.model.table;

import lombok.EqualsAndHashCode;
import omegapoint.opera.operationaljournal.domain.model.*;
import org.apache.commons.lang3.Validate;
import reactor.util.annotation.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.Validate.notNull;

@EqualsAndHashCode
public final class RunItem {
    public final UUID runID;
    public final UUID parentId;
    public final Instant originTimestamp;
    public final WebhookStep step;
    public final ConflictId conflictId;
    public final QueueCheckpoint queueCheckpoint;

    public RunItem(final UUID runID,
                   final UUID parentId,
                   final Instant originTimestamp,
                   final WebhookStep step,
                   final ConflictId conflictId,
                   final QueueCheckpoint queueCheckpoint) {
        this.runID = notNull(runID);
        this.parentId = parentId;
        this.originTimestamp = notNull(originTimestamp);
        this.step = notNull(step);
        this.conflictId = notNull(conflictId);
        this.queueCheckpoint = notNull(queueCheckpoint);
    }

    public RunItem(final UUID runID,
                   @Nullable final UUID parentId,
                   final Instant originTimestamp,
                   final CheckpointType checkpointType,
                   final CheckpointPath checkpointPath,
                   final BlobReference blobReference,
                   final ConflictId conflictId,
                   final WebhookStep step) {
        this(runID,
                parentId,
                originTimestamp,
                step,
                conflictId,
                new QueueCheckpoint(checkpointPath, blobReference));

        Validate.isTrue(checkpointType.equals(CheckpointType.QUEUE));
    }

    public Optional<UUID> getParentId() {
        return Optional.ofNullable(parentId);
    }
}
