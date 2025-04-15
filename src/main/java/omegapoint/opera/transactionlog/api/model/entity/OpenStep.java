package omegapoint.opera.transactionlog.api.model.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.vavr.control.Either;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.transactionlog.config.JacksonConfig;
import omegapoint.opera.transactionlog.domain.model.valueobject.StepName;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import static org.apache.commons.lang3.Validate.notNull;

@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode
public class OpenStep {
    public final UUID transactionId;
    public final UUID parentId;
    public final String startTime;
    public final String name;

    @JsonCreator
    public OpenStep(@JsonProperty("transactionId") final UUID transactionId,
                    @JsonProperty("parentId") final UUID parentId, //TODO [ak, as]: Add @NonNull when implemented.
                    @JsonProperty("startTime") final String startTime,
                    @JsonProperty("name") final String name) {
        this.transactionId = notNull(transactionId);
        this.parentId = notNull(parentId);
        this.startTime = notNull(startTime);
        this.name = notNull(name);
    }

    public static Either<RejectMessage, omegapoint.opera.transactionlog.domain.model.entity.OpenStep> toDomain(final OpenStep openStep) {
        notNull(openStep);

        try {
            return Either.right(new omegapoint.opera.transactionlog.domain.model.entity.OpenStep(
                    openStep.transactionId,
                    openStep.parentId,
                    LocalDateTime.parse(openStep.startTime),
                    new StepName(openStep.name)
            ));
        } catch (IllegalArgumentException e) {
            return Either.left(RejectMessage.of400("Unable to turn the given Json into a domain object"));
            // TODO: 2024-02-20 [tw] can we figure out on more fine detail what went wrong, maybe distinguish Value obeject creation errors from others.
        } catch (DateTimeParseException e) {
            return Either.left(RejectMessage.of400("Date unable to be parsed"));
        } catch (Exception e) {
            return Either.left(RejectMessage.of400("Something else went wrong"));
        }
    }

    public static Either<RejectMessage, omegapoint.opera.transactionlog.api.model.entity.OpenStep> deserialize(final String jsonString) {
        notNull(jsonString);
        try {
            return Either.right(JacksonConfig.objectMapper().readValue(jsonString, omegapoint.opera.transactionlog.api.model.entity.OpenStep.class));
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

