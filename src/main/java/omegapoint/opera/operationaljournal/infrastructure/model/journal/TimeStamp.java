package omegapoint.opera.operationaljournal.infrastructure.model.journal;
import static org.apache.commons.lang3.Validate.*;

public class TimeStamp {
    public final String value;

    public TimeStamp(final String timeStamp) {
        String datePattern = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{7}";
        notNull(timeStamp, "Date string cannot be null");
        matchesPattern(timeStamp, datePattern, "Invalid date format: " + timeStamp);
        this.value = timeStamp;
    }
}
