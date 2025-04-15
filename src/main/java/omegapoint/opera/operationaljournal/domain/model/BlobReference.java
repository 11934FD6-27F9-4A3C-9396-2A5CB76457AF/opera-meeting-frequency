package omegapoint.opera.operationaljournal.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;

import java.util.Objects;

// TODO: 2024-02-27 [tw, mbloms] swap to valueobjects with validation.
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public final class BlobReference {
    public final String containerName;
    public final String path;

    @JsonCreator
    public BlobReference(@JsonProperty("containerName") final String containerName,@JsonProperty("path") final String path) {
        this.containerName = containerName;
        this.path = path;
    }
}
