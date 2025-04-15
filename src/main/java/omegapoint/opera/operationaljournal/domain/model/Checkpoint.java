package omegapoint.opera.operationaljournal.domain.model;

import lombok.EqualsAndHashCode;

import static org.apache.commons.lang3.Validate.notNull;

@EqualsAndHashCode
public final class Checkpoint {
    public final CheckpointPath checkpointPath;
    public final CheckpointType checkpointType;

    public Checkpoint(final CheckpointPath checkpointPath,
                      final CheckpointType checkpointType) {
        this.checkpointPath = notNull(checkpointPath);
        this.checkpointType = notNull(checkpointType);
    }
}
