package omegapoint.opera.operationaljournal.domain.model;

import lombok.EqualsAndHashCode;

import static org.apache.commons.lang3.Validate.notNull;

@EqualsAndHashCode
public final class CheckpointPath {
    public final String value;

    public CheckpointPath(final String value) {
        this.value = notNull(value);
    }
    // TODO: 2024-03-18 [tw] add validation, not too long and perhaps can also verify which queue names are valid
    // TODO: 2024-03-18 [tw] also consider making something like CheckPointQueueName that extends or implements this since Queue and URL are different concepts and need different validation. 
}
