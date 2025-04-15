package omegapoint.opera.transactionlog.domain.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;


@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode
public class BlobPath {
    public final String value;

    public BlobPath(@NonNull final String value) {
        if(value.length() >= 4000){
            this.value = value.substring(0, 4000);
        }
        else {
            this.value = value;
        }
    }
 }
