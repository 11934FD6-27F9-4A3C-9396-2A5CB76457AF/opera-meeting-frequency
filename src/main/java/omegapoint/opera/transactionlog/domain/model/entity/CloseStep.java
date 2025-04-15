package omegapoint.opera.transactionlog.domain.model.entity;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import omegapoint.opera.transactionlog.domain.model.valueobject.StepName;

import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode
public class CloseStep {
    public final UUID transactionId;
    public final UUID parentId;
    public final LocalDateTime endTime;
    public final StepName name;

    public CloseStep(final UUID transactionId,final UUID parentId,final LocalDateTime endTime,final StepName name) {
        this.transactionId = transactionId;
        this.parentId = parentId;
        this.endTime = endTime;
        this.name = name;
    }
}
