package omegapoint.opera.operationaljournal.domain.model;

import omegapoint.opera.operationaljournal.domain.model.table.JournalItem;
import omegapoint.opera.operationaljournal.domain.model.table.RunItem;

public interface StartItem {
    JournalItem toJournalItem();
    RunItem toRunItem();
}
