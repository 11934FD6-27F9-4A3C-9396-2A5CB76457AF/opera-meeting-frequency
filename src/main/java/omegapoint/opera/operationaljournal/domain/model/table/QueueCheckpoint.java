package omegapoint.opera.operationaljournal.domain.model.table;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import omegapoint.opera.operationaljournal.domain.model.BlobReference;
import omegapoint.opera.operationaljournal.domain.model.CheckpointPath;

import java.util.Objects;

import static org.apache.commons.lang3.Validate.notNull;

@EqualsAndHashCode
public final class QueueCheckpoint {
    public final CheckpointPath queueName;
    public final BlobReference blobReference;

    public QueueCheckpoint(final CheckpointPath queueName,
                           final BlobReference blobReference) {
        this.queueName = notNull(queueName);
        this.blobReference = notNull(blobReference);
    }
}
