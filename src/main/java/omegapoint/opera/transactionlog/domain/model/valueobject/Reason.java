package omegapoint.opera.transactionlog.domain.model.valueobject;

public enum Reason {
    PARSING_ERROR,
    HTTP_ERROR,
    HTTP_CLIENT_ERROR,
    HTTP_SERVER_ERROR,
    SQL_ERROR,
    OTHER,
    NOT_APPLICABLE,
    INGESTION_ERROR,
    DSR_ERROR,
    SOURCE_SYSTEM_ERROR,
    CSV_ARCHIVE_ERROR,
    INCORRECT_CALL_TO_DSR,
    INCORRECT_CALL_TO_SOURCE_SYSTEM;
}
