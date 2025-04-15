package omegapoint.opera.transactionlog.domain.model.entity;

import lombok.EqualsAndHashCode;
import omegapoint.opera.transactionlog.domain.model.valueobject.BlobPath;
import omegapoint.opera.transactionlog.domain.model.valueobject.NumberOfRecords;
import omegapoint.opera.transactionlog.domain.model.valueobject.Reason;

import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode
public class CloseTransaction {
    public final UUID id;
    public final LocalDateTime endTime;
    public final NumberOfRecords numberOfRecords;
    public final BlobPath blobPath;
    public final Boolean isSuccess;
    public final Reason reason;

    public CloseTransaction(final UUID id,
                            final LocalDateTime endTime,
                            final NumberOfRecords numberOfRecords,
                            final BlobPath blobPath,
                            final Boolean isSuccess,
                            final Reason reason) {
        this.id = id;
        this.endTime = endTime;
        this.numberOfRecords = numberOfRecords;
        this.blobPath = blobPath;
        this.isSuccess = isSuccess;
        this.reason = reason;
    }
}
