package omegapoint.opera.operationaljournal.api.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.vavr.control.Either;
import lombok.EqualsAndHashCode;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.operationaljournal.domain.model.table.ConflictItem;

@EqualsAndHashCode
@JsonIgnoreProperties
public class ConflictRequest {

    public final String runID;
    public final ConflictBody body;

    //NOTE: The absence of JSON-annotations is intentional. This class is not a JSON model.
    public ConflictRequest(final String runID,
                           final ConflictBody body) {
        this.runID = runID;
        this.body = body;
    }

    public Either<RejectMessage, ConflictItem> toConflictItem() {
        try {
            return Either.right(new ConflictItem(runID, body.conflictID));
        } catch (Exception e) {
            return Either.left(RejectMessage.of400("Faulty conflict request"));
        }
    }
}
