package omegapoint.opera.operationaljournal.domain.model;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public final class ConflictId {
    public final String id;

    public ConflictId(String id) {
        this.id = id;
    }
}
