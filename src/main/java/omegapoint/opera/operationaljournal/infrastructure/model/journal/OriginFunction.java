package omegapoint.opera.operationaljournal.infrastructure.model.journal;

import static org.apache.commons.lang3.Validate.isTrue;

public class OriginFunction {
    public final String value;

    public OriginFunction(final String originFunction) {
        if (originFunction != null) {
            isTrue(originFunction.length()<200, "length should be less than 200 characters");
        }
        this.value = originFunction;
    }
}
