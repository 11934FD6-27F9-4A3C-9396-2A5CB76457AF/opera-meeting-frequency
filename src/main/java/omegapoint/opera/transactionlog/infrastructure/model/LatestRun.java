package omegapoint.opera.transactionlog.infrastructure.model;


public class LatestRun{
        public final String ID;
        public final String StartTime;
        public final String Flow;
        public final String db;
        public final String EndTime;
        public final int NumberOfRecords;
        public final String BlobPath;
        public final Boolean IsSuccess;
        public final String IngestionResultMessage;
        public final String Status;
        public final String Origin;

        public LatestRun(String ID, String startTime, String flow, String db, String endTime, int numberOfRecords, String blobPath, Boolean isSuccess, String ingestionResultMessage, String status, String origin) {
                this.ID = ID;
                this.StartTime = startTime;
                this.Flow = flow;
                this.db = db;
                this.EndTime = endTime;
                this.NumberOfRecords = numberOfRecords;
                this.BlobPath = blobPath;
                this.IsSuccess = isSuccess;
                this.IngestionResultMessage = ingestionResultMessage;
                this.Status = status;
                this.Origin = origin;
        }
}
