package omegapoint.opera.transactionlog.api.model.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.vavr.control.Either;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.transactionlog.domain.model.valueobject.Database;
import omegapoint.opera.transactionlog.domain.model.valueobject.OperationsType;
import omegapoint.opera.transactionlog.domain.model.valueobject.Trigger;
import omegapoint.opera.transactionlog.config.JacksonConfig;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import static org.apache.commons.lang3.Validate.notNull;

@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenTransaction {
    public final UUID id;
    public final String startTime;
    public final String sourceSystem;
    public final String targetSystem;
    public final String flow;
    public final String database;
    public final String trigger;
    public final String operationsType;
    public final Boolean isRelevantForOptic;
    
    @JsonCreator
    public OpenTransaction(@JsonProperty("id")final  UUID id,
                           @JsonProperty("startTime")final  String startTime,
                           @JsonProperty("sourceSystem")final  String sourceSystem,
                           @JsonProperty("targetSystem")final  String targetSystem,
                           @JsonProperty("flow")final  String flow,
                           @JsonProperty("database")final  String database,
                           @JsonProperty("trigger")final  String trigger,
                           @JsonProperty("operationsType")final  String operationsType,
                           @JsonProperty("isRelevantForOptic") final Boolean isRelevantForOptic){
        this.id = notNull(id);
        this.startTime = notNull(startTime);
        this.sourceSystem = notNull(sourceSystem);
        this.targetSystem = notNull(targetSystem);
        this.flow = notNull(flow);
        this.database = notNull(database);
        this.trigger = notNull(trigger);;
        this.operationsType = notNull(operationsType);
        this.isRelevantForOptic = isRelevantForOptic;
    }

    public static Either<RejectMessage, omegapoint.opera.transactionlog.domain.model.entity.OpenTransaction> toDomain(final OpenTransaction openTransaction) {
       notNull(openTransaction);
        try {
            return Either.right(new omegapoint.opera.transactionlog.domain.model.entity.OpenTransaction(
                    openTransaction.id,
                    LocalDateTime.parse(openTransaction.startTime),
                    openTransaction.sourceSystem,
                    openTransaction.targetSystem,
                    openTransaction.flow,
                    Database.valueOf(openTransaction.database),
                    Trigger.valueOf(openTransaction.trigger),
                    OperationsType.valueOf(openTransaction.operationsType),
                    openTransaction.isRelevantForOptic
            ));
        } catch (IllegalArgumentException e) {
            return Either.left(RejectMessage.of400("Unable to turn the given Json into a domain object"));
        } catch (DateTimeParseException e) {
            return Either.left(RejectMessage.of400("Date unable to be parsed"));
        } catch (Exception e) {
            return Either.left(RejectMessage.of400("Something else went wrong"));
        }
    }

    public static Either<RejectMessage, OpenTransaction> deserialize(String jsonString) {
        notNull(jsonString);
        
        try {
            return Either.right(JacksonConfig.objectMapper().readValue(jsonString, OpenTransaction.class));
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
