package omegapoint.opera.operationaljournal.api.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.vavr.control.Either;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.transactionlog.config.JacksonConfig;

import java.util.UUID;

@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConflictBody {

    public final String conflictID;

    public ConflictBody(@JsonProperty("conflictID") @NonNull final String conflictID) {
        this.conflictID = conflictID;
    }

    public ConflictRequest toConflictRequest(String runID) {
        return new ConflictRequest(runID, this);
    }

    public static Either<RejectMessage, ConflictBody> deserialize(final String body) {
        try {
            return Either.right(JacksonConfig.objectMapper().readValue(body, ConflictBody.class));
        } catch (JsonProcessingException e) {
            if (e.getCause() instanceof NullPointerException) {
                return Either.left(RejectMessage.of400(
                        "Some field is null."
                ));
            } else {
                return Either.left(RejectMessage.of500(
                        "Could not parse the message."
                ));
            }
        }
    }
}
