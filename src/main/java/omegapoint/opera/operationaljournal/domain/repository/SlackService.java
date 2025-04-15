package omegapoint.opera.operationaljournal.domain.repository;

import io.vavr.control.Either;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.logging.SuccessMessage;
import omegapoint.opera.operationaljournal.domain.model.table.JournalErrorItem;

import java.util.List;


public interface SlackService {
    Either<RejectMessage, SuccessMessage> sendAlert(final List<JournalErrorItem> journalErrorItem);
}
