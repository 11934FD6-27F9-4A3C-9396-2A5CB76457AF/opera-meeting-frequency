package omegapoint.opera.transactionlog.domain.model.entity;

import lombok.EqualsAndHashCode;
import omegapoint.opera.transactionlog.domain.model.valueobject.Database;
import omegapoint.opera.transactionlog.domain.model.valueobject.OperationsType;
import omegapoint.opera.transactionlog.domain.model.valueobject.Trigger;

import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode
public class OpenTransaction {
    public final UUID id;
    public final LocalDateTime startTime;
    public final String sourceSystem;
    public final String targetSystem;
    public final String flow;
    public final Database database;
    public final Trigger trigger;
    public final OperationsType operationsType;
    public final Boolean isRelevantForOptic;

    // TODO: 2024-02-06 [tw] think about validation for this entire entity / valueobjects
    public OpenTransaction(final UUID id,
                           final LocalDateTime startTime,
                           final String sourceSystem,
                           final String targetSystem,
                           final String flow,
                           final Database database,
                           final Trigger trigger,
                           final OperationsType operationsType, 
                           final Boolean isRelevantForOptic) {
        this.id = id;
        this.sourceSystem = sourceSystem;
        this.targetSystem = targetSystem;
        this.flow = flow;
        this.database = database;
        this.trigger = trigger;
        this.startTime = startTime;
        this.operationsType = operationsType;
        this.isRelevantForOptic = isRelevantForOptic;
    }
}