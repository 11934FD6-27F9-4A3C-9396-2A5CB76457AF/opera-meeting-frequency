package omegapoint.opera.transactionlog.domain.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;

@EqualsAndHashCode
@ToString(onlyExplicitlyIncluded = true)
public class StepName {
    
    public final String value;

    public StepName(String value) {
        notNull(value);
        isTrue(value.length() <= 50);
        this.value = value;
    }
}
