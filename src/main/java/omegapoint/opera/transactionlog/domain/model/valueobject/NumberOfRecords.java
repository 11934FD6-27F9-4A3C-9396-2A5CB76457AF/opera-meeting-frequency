package omegapoint.opera.transactionlog.domain.model.valueobject;

import lombok.EqualsAndHashCode;

import static org.apache.commons.lang3.Validate.isTrue;

@EqualsAndHashCode
public class NumberOfRecords {
    
    public final int value;
    
    public NumberOfRecords(final int value) {
        isTrue(value >= 0);
        this.value = value;
    }
}
