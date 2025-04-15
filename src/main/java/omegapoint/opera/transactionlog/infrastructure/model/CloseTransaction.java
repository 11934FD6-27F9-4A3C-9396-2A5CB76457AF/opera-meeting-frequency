package omegapoint.opera.transactionlog.infrastructure.model;

import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * Note that the field names begin with capital letters to match with the database. This is a feature.
 */
@EqualsAndHashCode
public class CloseTransaction {
    
    public final UUID ID;
    public final String EndTime;
    public final String NumberOfRecords;
    public final String BlobPath;
    public final String IsSuccess;
    public final String Reason;

    public CloseTransaction(
            final UUID ID,
            final String endTime,
            final String numberOfRecords,
            final String blobPath,
            final String isSuccess,
            final String reason) {
        this.ID = ID;
        this.EndTime = endTime;
        this.NumberOfRecords = numberOfRecords;
        this.BlobPath = blobPath;
        this.IsSuccess = isSuccess;
        this.Reason = reason;
    }

    public static CloseTransaction fromDomain(omegapoint.opera.transactionlog.domain.model.entity.CloseTransaction closeTransaction) {
        return new CloseTransaction(
                closeTransaction.id,
                closeTransaction.endTime.toString(),
                Integer.toString(closeTransaction.numberOfRecords.value),
                closeTransaction.blobPath.value,
                Boolean.toString(closeTransaction.isSuccess),
                closeTransaction.reason.toString());
    }
}
