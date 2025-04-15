package omegapoint.opera.transactionlog.infrastructure.model;

import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * Note that the field names begin with capital letters to match with the database. This is a feature.
 */
@EqualsAndHashCode
public class OpenTransaction {
    public final UUID ID;
    public final String StartTime;
    public final String SourceSystem;
    public final String TargetSystem;
    public final String Flow;
    public final String Database;
    public final String Trigger;
    public final String OperationsType;
    public final Boolean isRelevantForOptic;


    public OpenTransaction(final UUID id,
                           final String startTime,
                           final String sourceSystem,
                           final String targetSystem,
                           final String flow,
                           final String database,
                           final String trigger,
                           final String operationsType,
                           final Boolean isRelevantForOptic) {
        this.ID = id;
        this.SourceSystem = sourceSystem;
        this.TargetSystem = targetSystem;
        this.Flow = flow;
        this.Database = database;
        this.Trigger = trigger;
        this.StartTime = startTime;
        this.OperationsType = operationsType;
        this.isRelevantForOptic = isRelevantForOptic;
    }

    public static OpenTransaction fromDomain(final omegapoint.opera.transactionlog.domain.model.entity.OpenTransaction openTransaction) {
        return new OpenTransaction(openTransaction.id,
                openTransaction.startTime.toString(),
                openTransaction.sourceSystem.toString(),
                openTransaction.targetSystem.toString(),
                openTransaction.flow.toString(),
                openTransaction.database.toString(),
                openTransaction.trigger.toString(),
                openTransaction.operationsType.toString(),
                openTransaction.isRelevantForOptic);
    }
}
