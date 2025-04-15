package omegapoint.opera.operationaljournal.infrastructure.model;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class ConflictItem {
    public final String RunID;
    public final String ConflictID;

    public ConflictItem(final String runID, final String conflictID) {
        RunID = runID;
        ConflictID = conflictID;
    }

    public static ConflictItem fromDomain(final omegapoint.opera.operationaljournal.domain.model.table.ConflictItem domain) {
        return new ConflictItem(
                domain.runID(),
                domain.conflictID()
        );
    }
}
