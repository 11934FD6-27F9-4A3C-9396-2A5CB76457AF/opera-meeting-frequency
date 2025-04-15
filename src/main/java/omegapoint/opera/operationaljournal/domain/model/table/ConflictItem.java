package omegapoint.opera.operationaljournal.domain.model.table;

import lombok.EqualsAndHashCode;
import lombok.NonNull;

public record ConflictItem(@NonNull String runID, @NonNull String conflictID) {}
