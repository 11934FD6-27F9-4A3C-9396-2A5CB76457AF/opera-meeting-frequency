package omegapoint.opera.transactionlog.api.model.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.vavr.control.Either;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.transactionlog.domain.model.valueobject.*;
import omegapoint.opera.transactionlog.config.JacksonConfig;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import static org.apache.commons.lang3.Validate.*;
import static org.apache.commons.lang3.Validate.notNull;

@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode
public class CloseStep {
    public final UUID transactionId;
    public final UUID parentId;
    public final String endTime;
    public final String name;

    @JsonCreator
    public CloseStep(@JsonProperty("transactionId") UUID transactionId,
                     @JsonProperty("parentId") UUID parentId,
                     @JsonProperty("endTime") String endTime,
                     @JsonProperty("name") String name) {
        this.transactionId = notNull(transactionId);
        this.parentId = notNull(parentId);
        this.endTime = notNull(endTime);
        this.name = notNull(name);
    }

    public static Either<RejectMessage, omegapoint.opera.transactionlog.domain.model.entity.CloseStep> toDomain(final CloseStep closeStep) {
       notNull(closeStep);
        try {
            return Either.right(new omegapoint.opera.transactionlog.domain.model.entity.CloseStep(
                    closeStep.transactionId,
                    closeStep.parentId,
                    LocalDateTime.parse(closeStep.endTime),
                    new StepName(closeStep.name)
            ));
        } catch (IllegalArgumentException e) {
            return Either.left(RejectMessage.of400("Unable to turn the given Json into a domain object"));
        } catch (DateTimeParseException e) {
            return Either.left(RejectMessage.of400("Date unable to be parsed"));
        } catch (Exception e) {
            return Either.left(RejectMessage.of400("Something else went wrong"));
        }
    }

    public static Either<RejectMessage, omegapoint.opera.transactionlog.api.model.entity.CloseStep> deserialize(final String jsonString) {
       notNull(jsonString);
       
        try {
            return Either.right(JacksonConfig.objectMapper().readValue(jsonString, omegapoint.opera.transactionlog.api.model.entity.CloseStep.class));
        } catch (JsonProcessingException e) {
            if (e.getCause() instanceof DateTimeParseException parseException) {
                return Either.left(RejectMessage.of400(
                        "%s is not a date time with a time zone. Use java.time.Instant or java.time.ZonedDateTime."
                                .formatted(parseException.getParsedString())));
            } else if (e.getCause() instanceof NullPointerException) {
                return Either.left(RejectMessage.of400("Some field is null: "));
            }
            return Either.left(RejectMessage.of400(e.getMessage()));
        }
    }

}