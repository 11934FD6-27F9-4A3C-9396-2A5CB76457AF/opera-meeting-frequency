package omegapoint.opera.operationaljournal.infrastructure.model;

import lombok.EqualsAndHashCode;

// Java -> SQL Types (https://learn.microsoft.com/en-us/sql/connect/jdbc/using-basic-data-types?view=sql-server-ver16)

@EqualsAndHashCode
public class JournalItem {
    public final String RunID;
    public final Integer Attempt;
    public final String Timestamp;
    public final String Status;
    public final String Message;
    public final String OriginFunction;

    public JournalItem(final String runID, final Integer Attempt, final String timestamp, final String status, final String message, final String originFunction) {
        this.RunID = runID;
        this.Attempt = Attempt;
        this.Timestamp = timestamp;
        this.Status = status;
        this.Message = message;
        this.OriginFunction = originFunction;
    }

    public static JournalItem fromDomain(final omegapoint.opera.operationaljournal.domain.model.table.JournalItem domain) {
        return new JournalItem(
                domain.runID,
                domain.attempt,
                SqlTimestamp.fromInstant(domain.timestamp).toSqlString(),
                domain.status.toString(),
                domain.message,
                domain.originFunction);
    }

    public omegapoint.opera.operationaljournal.domain.model.table.JournalItem toDomain() {
        return new omegapoint.opera.operationaljournal.domain.model.table.JournalItem(
                this.RunID,
                this.Attempt,
                SqlTimestamp.parse(this.Timestamp).toInstant(),
                omegapoint.opera.operationaljournal.domain.model.Status.valueOf(Status),
                this.Message,
                this.OriginFunction
        );
    }
}
