package omegapoint.opera.operationaljournal.api.model.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import omegapoint.opera.operationaljournal.domain.model.CheckpointPath;
import omegapoint.opera.operationaljournal.domain.model.CheckpointType;

import static org.apache.commons.lang3.Validate.notNull;
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class Checkpoint {

    public final String checkpointPath;
    public final String checkpointType;

    @JsonCreator
    public Checkpoint(@JsonProperty("checkpointPath") final String checkpointPath,
                      @JsonProperty("checkpointType") final String checkpointType) {
        this.checkpointPath = notNull(checkpointPath);
        this.checkpointType = notNull(checkpointType);
    }
    
    public omegapoint.opera.operationaljournal.domain.model.Checkpoint toDomain() {
        return new omegapoint.opera.operationaljournal.domain.model.Checkpoint(
                new CheckpointPath(checkpointPath),
                CheckpointType.valueOf(checkpointType));
    }
}