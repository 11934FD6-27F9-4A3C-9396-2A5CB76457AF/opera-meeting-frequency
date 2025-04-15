package omegapoint.opera.operationaljournal.infrastructure.model;

import lombok.EqualsAndHashCode;
import omegapoint.opera.operationaljournal.domain.model.BlobReference;
import omegapoint.opera.operationaljournal.domain.model.CheckpointPath;

@EqualsAndHashCode
public class QueueCheckpoint {
    public final String RunID;
    public final String QueueName;
    public final String BlobContainerName;
    public final String BlobPath;

    QueueCheckpoint(final String runID, final String queueName, final String blobContainerName, final String blobPath) {
        this.RunID = runID;
        this.QueueName = queueName;
        this.BlobContainerName = blobContainerName;
        this.BlobPath = blobPath;
    }

    public omegapoint.opera.operationaljournal.domain.model.table.QueueCheckpoint toDomain() {
        return new omegapoint.opera.operationaljournal.domain.model.table.QueueCheckpoint(
                new CheckpointPath(this.QueueName),
                new BlobReference(
                        this.BlobContainerName,
                        this.BlobPath));
    }
}
