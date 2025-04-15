package omegapoint.opera.transactionlog.infrastructure.model;

import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * Note that the field names begin with capital letters to match with the database. This is a feature.
 */
@EqualsAndHashCode 
public class OpenStep {
    
    public final UUID TransactionID;
    public final UUID ParentID;
    public final String StartTime;
    public final String Name;

    public OpenStep(final UUID transactionId,final UUID parentId,final String startTime,final String name) {
        this.TransactionID = transactionId;
        this.ParentID = parentId;
        this.StartTime = startTime;
        this.Name = name;
    }

    public static OpenStep fromDomain(final omegapoint.opera.transactionlog.domain.model.entity.OpenStep openStep) {
        return new OpenStep(
                openStep.transactionId,
                openStep.parentId,
                openStep.startTime.toString(),
                openStep.name.value);
    }
}
