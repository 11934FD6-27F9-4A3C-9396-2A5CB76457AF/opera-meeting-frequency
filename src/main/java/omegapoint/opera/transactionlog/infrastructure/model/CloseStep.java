package omegapoint.opera.transactionlog.infrastructure.model;

import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * Note that the field names begin with capital letters to match with the database. This is a feature.
 */
@EqualsAndHashCode
public class CloseStep {
    public final UUID TransactionID;
    public final UUID ParentID;
    public final String EndTime;
    public final String Name;

    public CloseStep(final UUID transactionId,final UUID parentId,final String endTime,final String name) {
        this.TransactionID = transactionId;
        this.ParentID = parentId;
        this.EndTime = endTime;
        this.Name = name;
    }

    public static CloseStep fromDomain(final omegapoint.opera.transactionlog.domain.model.entity.CloseStep closeStep) {
        return new CloseStep(
                closeStep.transactionId,
                closeStep.parentId,
                closeStep.endTime.toString(),
                closeStep.name.value);
    }
}