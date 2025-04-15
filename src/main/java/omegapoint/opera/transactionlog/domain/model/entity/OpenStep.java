package omegapoint.opera.transactionlog.domain.model.entity;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import omegapoint.opera.transactionlog.domain.model.valueobject.StepName;

import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode
public class OpenStep {
    
    public final UUID transactionId;
    public final UUID parentId;
    public final LocalDateTime startTime;
    public final StepName name;

    public OpenStep(final UUID transactionId,final UUID parentId,final LocalDateTime startTime,final StepName name) {
        this.transactionId = transactionId;
        this.parentId = parentId;
        this.startTime = startTime;
        this.name = name;
    }
}
