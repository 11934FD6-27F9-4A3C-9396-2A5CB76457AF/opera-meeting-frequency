package omegapoint.opera.operationaljournal.infrastructure;

import io.vavr.control.Either;
import lombok.NonNull;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.logging.SuccessMessage;
import omegapoint.opera.operationaljournal.domain.model.StartItem;
import omegapoint.opera.operationaljournal.infrastructure.model.*;
import omegapoint.opera.operationaljournal.infrastructure.model.journal.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class DatabaseRepository implements omegapoint.opera.operationaljournal.domain.repository.DatabaseRepository {

    private final DataSource dataSource;
    private final Logger logger;

    public DatabaseRepository(@NonNull final DataSource dataSource,
                              final @NonNull Logger logger) {
        this.dataSource = dataSource;
        this.logger = logger;
    }

    public Either<RejectMessage, SuccessMessage> putStart(StartItem startItem) {
        final var tuple = RunItem.fromDomain(startItem.toRunItem());
        final RunItem runItem = tuple._1();
        final QueueCheckpoint queueCheckpoint = tuple._2();
        final JournalItem journalItem = JournalItem.fromDomain(startItem.toJournalItem());
        final String runId = journalItem.RunID;

        try (Connection connection = dataSource.getConnection()) {
            String insertSql = """
                    BEGIN TRANSACTION;
                    
                    DECLARE @AffectedRows INT = 0;
                    
                    IF NOT EXISTS (SELECT * FROM OperationsJournal.Journal WITH (UPDLOCK, HOLDLOCK) WHERE RunID = ?)
                    AND NOT EXISTS (SELECT * FROM OperationsJournal.Run WITH (UPDLOCK, HOLDLOCK) WHERE RunID = ?)
                    BEGIN
                        INSERT INTO OperationsJournal.Run (RunID, ParentID, OriginTimestamp, WebhookStep) VALUES (?, ?, ?, ?);
                        SET @AffectedRows = @AffectedRows + @@ROWCOUNT;
                        
                        INSERT INTO OperationsJournal.Journal (RunID, Timestamp, Status, Message, OriginFunction, Attempt) VALUES (?, ?, ?, ?, ?, ?);
                        SET @AffectedRows = @AffectedRows + @@ROWCOUNT;
                        
                        INSERT INTO OperationsJournal.QueueCheckpoint (RunID, QueueName, BlobContainerName, BlobPath) VALUES (?, ?, ?, ?);
                        SET @AffectedRows = @AffectedRows + @@ROWCOUNT;
                    END;
                    
                    COMMIT TRANSACTION;
                    
                    SELECT @AffectedRows AS AffectedRows;
                    """;
            PreparedStatement statement = connection.prepareStatement(insertSql);

            statement.setString(1, runId);
            statement.setString(2, runId);

            statement.setString(3, runItem.RunID);
            statement.setString(4, runItem.ParentID);
            statement.setString(5, runItem.OriginTimestamp);
            statement.setString(6, runItem.WebhookStep);

            statement.setString(7, journalItem.RunID);
            statement.setString(8, journalItem.Timestamp);
            statement.setString(9, journalItem.Status);
            statement.setString(10, journalItem.Message);
            statement.setString(11, journalItem.OriginFunction);
            statement.setString(12, String.valueOf(journalItem.Attempt));

            statement.setString(13, queueCheckpoint.RunID);
            statement.setString(14, queueCheckpoint.QueueName);
            statement.setString(15, queueCheckpoint.BlobContainerName);
            statement.setString(16, queueCheckpoint.BlobPath);


            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                int affectedRows = rs.getInt("AffectedRows");
                if (affectedRows == 0) {
                    logger.warning("Condition in SQL were false, no records inserted.");
                    return Either.left(RejectMessage.of400("Run has already been started."));
                } else {
                    logger.info("Rows successfully inserted. Rows affected: " + String.valueOf(affectedRows));
                    return Either.right(SuccessMessage.of("Rows successfully inserted."));
                }
            } else {
                return Either.left(RejectMessage.of500("Database query failed."));
            }
        } catch (SQLException e) {
            logger.severe(e.getMessage());
            return Either.left(RejectMessage.of500(e.getMessage()));
        }
    }

    @Override
    public Either<RejectMessage, SuccessMessage> putStop(omegapoint.opera.operationaljournal.domain.model.table.JournalItem domainJournalItem) {
        JournalItem journalItem = JournalItem.fromDomain(domainJournalItem);

        try (Connection connection = dataSource.getConnection()) {
            String insertSql = """
                    INSERT INTO OperationsJournal.Journal (RunID, Timestamp, Status, Message, OriginFunction, Attempt) VALUES (?, ?, ?, ?, ?, ?);
                    """;
            PreparedStatement insertStatement = connection.prepareStatement(insertSql);

            insertStatement.setString(1, journalItem.RunID);
            insertStatement.setString(2, journalItem.Timestamp);
            insertStatement.setString(3, journalItem.Status);
            insertStatement.setString(4, journalItem.Message);
            insertStatement.setString(5, journalItem.OriginFunction);
            insertStatement.setString(6, String.valueOf(journalItem.Attempt));

            int affectedRows = insertStatement.executeUpdate();
            if (affectedRows != 1) {
                logger.severe("Insertion failed without SQLException.");
                return Either.left(RejectMessage.of500("Insertion failed without SQLException."));
            }

            String selectSql = """
                    SELECT COUNT(*) FROM OperationsJournal.Journal WHERE RunID = ? AND Status = ? AND Attempt = ?;
                    """;
            PreparedStatement selectStatement = connection.prepareStatement(selectSql);

            selectStatement.setString(1, journalItem.RunID);
            selectStatement.setString(2, journalItem.Status);
            selectStatement.setString(3, String.valueOf(journalItem.Attempt));

            ResultSet resultSet = selectStatement.executeQuery();
            if (resultSet.next()) {
                int recordsFound = resultSet.getInt(1);
                if (recordsFound > 1) {
                    logger.severe("Found duplicate records of Journal for runId: " + journalItem.RunID);
                    logger.warning("Number of records: " + String.valueOf(recordsFound));
                    return Either.right(SuccessMessage.of("Successful insert with duplicate records."));
                } else return Either.right(SuccessMessage.of("Row successfully inserted."));
            } else {
                return Either.left(RejectMessage.of500("Failed to count existing records."));
            }
        } catch (SQLException e) {
            logger.severe(e.getMessage());
            return Either.left(RejectMessage.of500(e.getMessage()));
        }
    }

    @Override
    public Either<RejectMessage, SuccessMessage> putRestart(omegapoint.opera.operationaljournal.domain.model.table.JournalItem domainJournalItem) {
        JournalItem journalItem = JournalItem.fromDomain(domainJournalItem);

        try (Connection connection = dataSource.getConnection()) {
            final String insertSql = """
                    BEGIN TRANSACTION;
                    DECLARE @ResultCode INT = 0;
                    IF NOT EXISTS (SELECT * FROM OperationsJournal.Journal WITH (UPDLOCK, HOLDLOCK) WHERE RunID = ? AND Attempt = ?)
                    BEGIN
                        IF EXISTS (SELECT * FROM OperationsJournal.Run WITH (UPDLOCK, HOLDLOCK) WHERE RunID = ?)
                        BEGIN
                            INSERT INTO OperationsJournal.Journal (RunID, Timestamp, Status, Message, OriginFunction, Attempt) 
                            VALUES (?, ?, ?, ?, ?, ?);
                        END
                        ELSE
                        BEGIN
                            SET @ResultCode = 2; -- RunID does not exist
                        END;
                    END
                    ELSE
                    BEGIN
                        SET @ResultCode = 1; -- Record already exists
                    END;
                    COMMIT TRANSACTION;
                    SELECT @ResultCode AS ResultCode;
                    """;
            PreparedStatement statement = connection.prepareStatement(insertSql);

            statement.setString(1, journalItem.RunID);
            statement.setString(2, String.valueOf(journalItem.Attempt));

            statement.setString(3, journalItem.RunID);

            statement.setString(4, journalItem.RunID);
            statement.setString(5, journalItem.Timestamp);
            statement.setString(6, journalItem.Status);
            statement.setString(7, journalItem.Message);
            statement.setString(8, journalItem.OriginFunction);
            statement.setString(9, String.valueOf(journalItem.Attempt));

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int resultCode = resultSet.getInt("ResultCode");
                return switch (resultCode) {
                    case 0 -> Either.right(SuccessMessage.of("Row successfully inserted"));
                    case 1 ->
                            Either.left(RejectMessage.of409("Could not insert the row, restart with same attempt already exists."));
                    case 2 -> Either.left(RejectMessage.of400("Run does not exist."));
                    default -> {
                        logger.severe("Unknown error in putRestart.");
                        yield Either.left(RejectMessage.of500("Unknown error occurred."));
                    }
                };
            } else return Either.left(RejectMessage.of500("Writing to database failed."));
        } catch (SQLException e) {
            logger.severe(e.getMessage());
            return Either.left(RejectMessage.of500(e.getMessage()));
        }
    }

    public Either<RejectMessage, SuccessMessage> putRunAndQueueCheckpoint(omegapoint.opera.operationaljournal.domain.model.table.RunItem runItem) {
        final var tuple = RunItem.fromDomain(runItem);
        final RunItem outputRunItem = tuple._1();
        final QueueCheckpoint outputQueueCheckpoint = tuple._2();

        Either<RejectMessage, SuccessMessage> putRunResult = putRun(outputRunItem);
        if (putRunResult.isLeft()) {
            return Either.left(putRunResult.getLeft());
        }

        Either<RejectMessage, SuccessMessage> putQueueCheckpointResult = putQueueCheckpoint(outputQueueCheckpoint);
        if (putQueueCheckpointResult.isRight()) {
            return Either.right(SuccessMessage.of("Rows successfully inserted"));
        } else {
            return Either.left(RejectMessage.of500("Could not write to database"));
        }
    }

    @Override
    public Either<RejectMessage, SuccessMessage> putJournal(omegapoint.opera.operationaljournal.domain.model.table.JournalItem journalItem) {
        JournalItem outputJournalItem = JournalItem.fromDomain(journalItem);
        try (Connection connection = dataSource.getConnection()) {
            String insertSql = "INSERT INTO OperationsJournal.Journal (RunID, Timestamp, Status, Message, OriginFunction, Attempt) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(insertSql);
            statement.setString(1, outputJournalItem.RunID);
            statement.setString(2, outputJournalItem.Timestamp);
            statement.setString(3, outputJournalItem.Status);
            statement.setString(4, outputJournalItem.Message);
            statement.setString(5, outputJournalItem.OriginFunction);
            statement.setString(6, outputJournalItem.Attempt.toString());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 1) {
                return Either.right(SuccessMessage.of("Row successfully inserted."));
            } else {
                return Either.left(RejectMessage.of500("Inserting the database row failed."));
            }
        } catch (SQLException e) {
            return Either.left(RejectMessage.of500(e.getMessage()));
        }
    }

    @Override
    public Either<RejectMessage, SuccessMessage> putConflict(omegapoint.opera.operationaljournal.domain.model.table.ConflictItem conflictItem) {
        ConflictItem outputConflictItem = ConflictItem.fromDomain(conflictItem);
        try (Connection connection = dataSource.getConnection()) {
            // TODO: 2024-05-30 [mbloms, ls] Tillåt dubletter! Men skippa att lägga in dem dubbelt i databasen dvs om (RunId, ConflictId) redan finns, returnera OK
            String insertSql = """
                    BEGIN TRANSACTION;
                    DECLARE @ResultCode INT = 0;
                    IF NOT EXISTS (SELECT * FROM OperationsJournal.Conflict WITH (UPDLOCK, HOLDLOCK) WHERE RunID = ? AND ConflictID = ?)
                    BEGIN
                        INSERT INTO OperationsJournal.Conflict (RunID, ConflictID) VALUES (?, ?)
                    END
                    ELSE
                    BEGIN
                        SET @ResultCode = 1; -- Record already exists
                    END;
                    COMMIT TRANSACTION;
                    SELECT @ResultCode AS ResultCode;
                    """;
            PreparedStatement statement = connection.prepareStatement(insertSql);
            statement.setString(1, outputConflictItem.RunID);
            statement.setString(2, outputConflictItem.ConflictID);
            statement.setString(3, outputConflictItem.RunID);
            statement.setString(4, outputConflictItem.ConflictID);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int resultCode = resultSet.getInt("ResultCode");
                return switch (resultCode) {
                    case 0 -> Either.right(SuccessMessage.of("Row successfully inserted"));
                    case 1 ->
                            Either.right(SuccessMessage.of("Record already exists"));
                    default -> {
                        logger.severe("Unknown error in putConflict.");
                        yield Either.left(RejectMessage.of500("Unknown error occurred."));
                    }
                };
            } else return Either.left(RejectMessage.of500("Writing to database failed."));
        } catch (SQLException e) {
            return Either.left(RejectMessage.of500(e.getMessage()));
        }
    }

    @Override
    public Either<RejectMessage, RunItem[]> getExistingRun(final @NonNull String runId) {
        try (Connection connection = dataSource.getConnection()) {
            String insertSql = """
                            SELECT * FROM OperationsJournal.Run
                            WHERE RunID = ?
                            """;
            PreparedStatement statement = connection.prepareStatement(insertSql);
            statement.setString(1, runId);

            ResultSet resultSet = statement.executeQuery();
            List<RunItem> runItems = new ArrayList<>();
            while (resultSet.next()) {
                runItems.add(new RunItem(
                                resultSet.getString("RunID"),
                                resultSet.getString("ParentID"),
                                resultSet.getString("OriginTimestamp"),
                                resultSet.getString("WebhookStep"),
                                null
                        )
                );
            }

            return Either.right(runItems.toArray(new RunItem[0]));
        } catch (SQLException e) {
            return Either.left(RejectMessage.of500("Failed to check for existing Runs. DatabaseRepository: " + e.getMessage()));
        }
    }

    @Override
    public Either<RejectMessage, JournalItem[]> getExistingJournal(final @NonNull String runId) {
        try (Connection connection = dataSource.getConnection()) {
            String insertSql = """
                            SELECT * FROM OperationsJournal.Journal
                            WHERE RunID = ?
                            """;
            PreparedStatement statement = connection.prepareStatement(insertSql);
            statement.setString(1, runId);

            ResultSet resultSet = statement.executeQuery();
            List<JournalItem> journalItems = new ArrayList<>();
            while (resultSet.next()) {
                journalItems.add(new JournalItem(
                        resultSet.getString("RunID"),
                        resultSet.getInt("Attempt"),
                        resultSet.getString("Timestamp"),
                        resultSet.getString("Status"),
                        resultSet.getString("Message"),
                        resultSet.getString("OriginFunction")
                ));
            }

            return Either.right(journalItems.toArray(new JournalItem[0]));
        } catch (SQLException e) {
            return Either.left(RejectMessage.of500("Failed to check for existing Journal rows. DatabaseRepository: " + e.getMessage()));
        }
    }

    @Override
    public Either<RejectMessage, List<omegapoint.opera.operationaljournal.domain.model.table.Rerun>> getReruns() {
        try (Connection connection = dataSource.getConnection()) {
            String insertSql = """
                            SELECT run.RunID, Attempt, WebhookStep, run.OriginTimestamp, QueueName, BlobContainerName, BlobPath
                            FROM OperationsJournal.ForRerun_Rejected rej\s
                                        INNER JOIN OperationsJournal.Run run ON rej.RunID = run.RunID
                                        INNER JOIN OperationsJournal.MostRecentJournal mrj ON rej.RunID = mrj.RunID
                                        INNER JOIN OperationsJournal.QueueCheckpoint qc ON rej.RunID = qc.RunID
                            """;
            PreparedStatement statement = connection.prepareStatement(insertSql);

            ResultSet resultSet = statement.executeQuery();
            List<Rerun> reruns = new ArrayList<>();
            while (resultSet.next()) {
                reruns.add(new Rerun(
                        resultSet.getString("RunID"),
                        resultSet.getInt("Attempt"),
                        resultSet.getString("WebhookStep"),
                        resultSet.getString("OriginTimestamp"),
                        resultSet.getString("QueueName"),
                        resultSet.getString("BlobContainerName"),
                        resultSet.getString("BlobPath")
                ));
            }

            return Either.right(reruns.stream().map(Rerun::toDomain).toList());
        } catch (SQLException e) {
            return Either.left(RejectMessage.of500("DatabaseRepository: " + e.getMessage()));
        }
    }

    @Override
    public Either<RejectMessage, omegapoint.opera.operationaljournal.domain.model.table.Rerun> getRerunInfo(final @NonNull String runId) {
        try (Connection connection = dataSource.getConnection()) {
            String insertSql = """
                            SELECT run.RunID, Attempt, WebhookStep, OriginTimestamp, QueueName, BlobContainerName, BlobPath
                            FROM OperationsJournal.MostRecentJournal mrj
                                     INNER JOIN OperationsJournal.Run run ON run.RunID = mrj.RunID
                                     INNER JOIN OperationsJournal.QueueCheckpoint qc ON qc.RunID = run.RunID
                            WHERE run.RunID = ?
                            """;
            PreparedStatement statement = connection.prepareStatement(insertSql);
            statement.setString(1, runId);

            ResultSet resultSet = statement.executeQuery();
            List<Rerun> reruns = new ArrayList<>();
            while (resultSet.next()) {
                reruns.add(new Rerun(
                        resultSet.getString("RunID"),
                        resultSet.getInt("Attempt"),
                        resultSet.getString("WebhookStep"),
                        resultSet.getString("OriginTimestamp"),
                        resultSet.getString("QueueName"),
                        resultSet.getString("BlobContainerName"),
                        resultSet.getString("BlobPath")
                ));
            }

            if (reruns.size() == 1) {
                return Either.right(reruns.stream().map(Rerun::toDomain).toList().get(0));
            } else if (reruns.isEmpty()) {
                final String errorMessage = "The journal does not contain any Run with the RunID '%s'."
                        .formatted(runId);
                logger.warning(errorMessage);
                return Either.left(RejectMessage.of400(errorMessage));
            } else {
                final String errorMessage = "Multiple Runs found in journal with runId: '%s'."
                        .formatted(runId);
                logger.warning(errorMessage);
                return Either.left(RejectMessage.of500(errorMessage));
            }
        } catch (SQLException e) {
            return Either.left(RejectMessage.of500("DatabaseRepository: " + e.getMessage()));
        }
    }

    @Override
    public Either<RejectMessage, Rerun[]> getCinodeReruns() {
        try (Connection connection = dataSource.getConnection()) {
            String insertSql = """
                    SELECT run.RunID, Attempt, WebhookStep, OriginTimestamp, QueueName, BlobContainerName, BlobPath
                    FROM OperationsJournal.MostRecentJournal mrj
                        INNER JOIN OperationsJournal.Run run ON run.RunID = mrj.RunID
                        INNER JOIN OperationsJournal.QueueCheckpoint qc ON qc.RunID = run.RunID
                    WHERE WebhookStep IN ('UPDATE_CINODE_ABSENCE_WITH_EMPLOYMENT_START_DATE', 'NEW_EMPLOYEE_ABSENCE_IN_CINODE', 'UPDATE_CINODE_TEAM') AND
                          OriginTimestamp >= DATEADD(month, -2, GETUTCDATE()) AND
                          (Status = 'ERROR' OR
                          (Status IN ('STARTED', 'RESTARTED') AND
                              DATEDIFF(minute, Timestamp, SYSUTCDATETIME()) > 10))
                    ORDER BY OriginTimestamp;
                    """;
            PreparedStatement statement = connection.prepareStatement(insertSql);

            ResultSet resultSet = statement.executeQuery();
            List<Rerun> reruns = new ArrayList<>();
            while (resultSet.next()) {
                reruns.add(new Rerun(
                        resultSet.getString("RunID"),
                        resultSet.getInt("Attempt"),
                        resultSet.getString("WebhookStep"),
                        resultSet.getString("OriginTimestamp"),
                        resultSet.getString("QueueName"),
                        resultSet.getString("BlobContainerName"),
                        resultSet.getString("BlobPath")
                ));
            }

            return Either.right(reruns.toArray(new Rerun[0]));
        } catch (SQLException e) {
            return Either.left(RejectMessage.of500("DatabaseRepository: " + e.getMessage()));
        }
    }

    private Either<RejectMessage, SuccessMessage> putRun(RunItem outputRunItem) {
        try (Connection connection = dataSource.getConnection()) {
            String insertSql = "INSERT INTO OperationsJournal.Run (RunID, ParentID, OriginTimestamp, WebhookStep) VALUES (?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(insertSql);
            statement.setString(1, outputRunItem.RunID);
            statement.setString(2, outputRunItem.ParentID);
            statement.setString(3, outputRunItem.OriginTimestamp);
            statement.setString(4, outputRunItem.WebhookStep);


            int affectedRows = statement.executeUpdate();
            if (affectedRows == 1) {
                return Either.right(SuccessMessage.of("Row successfully inserted."));
            } else {
                return Either.left(RejectMessage.of500("Inserting the database row failed."));
            }
        } catch (SQLException e) {
            return Either.left(RejectMessage.of500(e.getMessage()));
        }
    }

    private Either<RejectMessage, SuccessMessage> putQueueCheckpoint(QueueCheckpoint outputQueueCheckpoint) {
        try (Connection connection = dataSource.getConnection()) {
            String insertSql = "INSERT INTO OperationsJournal.QueueCheckpoint (RunID, QueueName, BlobContainerName, BlobPath) VALUES (?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(insertSql);
            statement.setString(1, outputQueueCheckpoint.RunID);
            statement.setString(2, outputQueueCheckpoint.QueueName);
            statement.setString(3, outputQueueCheckpoint.BlobContainerName);
            statement.setString(4, outputQueueCheckpoint.BlobPath);

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 1) {
                return Either.right(SuccessMessage.of("Row successfully inserted."));
            } else {
                return Either.left(RejectMessage.of500("Inserting the database row failed."));
            }
        } catch (SQLException e) {
            return Either.left(RejectMessage.of500(e.getMessage()));
        }
    }

    @Override
    public Either<RejectMessage, SuccessMessage> restrictRerun(omegapoint.opera.operationaljournal.domain.model.table.JournalItem journalItem) {
        JournalItem outputJournalItem = JournalItem.fromDomain(journalItem);
        try (Connection connection = dataSource.getConnection()) {
            String insertSql = """
                    BEGIN TRANSACTION;
                    DECLARE @ResultCode INT = 0;
                    IF NOT EXISTS (SELECT * FROM OperationsJournal.Journal WITH (UPDLOCK, HOLDLOCK) WHERE RunID = ? AND Status = ?)
                    BEGIN
                        INSERT INTO OperationsJournal.Journal (RunID, Timestamp, Status, Message, OriginFunction, Attempt) VALUES (?, ?, ?, ?, ?, ?);
                    END
                    ELSE
                    BEGIN
                        SET @ResultCode = 1; -- Record already exists
                    END;
                    COMMIT TRANSACTION;
                    SELECT @ResultCode AS ResultCode;
                    """;
            PreparedStatement statement = connection.prepareStatement(insertSql);
            statement.setString(1, outputJournalItem.RunID);
            statement.setString(2, outputJournalItem.Status);

            statement.setString(3, outputJournalItem.RunID);
            statement.setString(4, outputJournalItem.Timestamp);
            statement.setString(5, outputJournalItem.Status);
            statement.setString(6, outputJournalItem.Message);
            statement.setString(7, outputJournalItem.OriginFunction);
            statement.setNull(8, Types.INTEGER);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int resultCode = resultSet.getInt("ResultCode");
                return switch (resultCode) {
                    case 0 -> Either.right(SuccessMessage.of("Row successfully inserted"));
                    case 1 ->
                            Either.left(RejectMessage.of409("Record already exists"));
                    default -> {
                        logger.severe("Unknown error in restrictRerun.");
                        yield Either.left(RejectMessage.of500("Unknown error occurred."));
                    }
                };
            } else return Either.left(RejectMessage.of500("Writing to database failed."));
        } catch (SQLException e) {
            return Either.left(RejectMessage.of500(e.getMessage()));
        }
    }

    @Override
    public Either<RejectMessage, Optional<UUID>> getRootRunId(final @NonNull UUID runId) {
        return recursiveGetRootRunId(runId)
                .flatMap(rootRunIdOptional -> rootRunIdOptional.map(rootRunId -> {
                            if (runId.equals(rootRunId)) return Either.<RejectMessage, Optional<UUID>>right(Optional.empty());
                            return Either.<RejectMessage, Optional<UUID>>right(Optional.of(rootRunId));
                        })
                        .orElseGet(() -> Either.left(RejectMessage.of500("Error in recursiveGetRootRunId."))));
    }

    private Either<RejectMessage, Optional<UUID>> recursiveGetRootRunId(@NonNull UUID runId) {
        return getParentRunId(runId)
                .flatMap(result -> result
                        .map(parentRunId -> recursiveGetRootRunId(parentRunId) // Recursive call until root
                                .flatMap(Either::right))
                        .orElseGet(() -> Either.right(Optional.of(runId))));
    }

    private Either<RejectMessage, Optional<UUID>> getParentRunId(@NonNull UUID runId) {
        try (Connection connection = dataSource.getConnection()) {
            String sqlString = """
                    SELECT ParentID FROM OperationsJournal.Run WHERE RunID = ?
                    """;

            PreparedStatement statement = connection.prepareStatement(sqlString);
            statement.setString(1, runId.toString());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return Optional.ofNullable(resultSet.getString("ParentID"))
                        .map(parentRunId -> Either.<RejectMessage, Optional<UUID>>right(Optional.of(UUID.fromString(parentRunId))))
                        .orElseGet(() -> Either.right(Optional.empty()));
            }
            return Either.right(Optional.empty());
        } catch (SQLException e) {
            return Either.left(RejectMessage.of500(e.getMessage()));
        }
    }

    @Override
    public Either<RejectMessage, String> getWebhookStep(final @NonNull UUID runId) {
        try (Connection connection = dataSource.getConnection()) {
            String sqlString = """
                    SELECT WebhookStep
                      FROM OperationsJournal.Run
                      WHERE RunID = ?
                    """;

            PreparedStatement statement = connection.prepareStatement(sqlString);
            statement.setString(1, runId.toString());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return Either.right(resultSet.getString("WebhookStep"));
            }
            return Either.left(RejectMessage.of500("Could not find webhook step for runId: " + runId));
        } catch (SQLException e) {
            return Either.left(RejectMessage.of500(e.getMessage()));
        }
    }

    @Override
    public Either<RejectMessage, String> getParentWebhookStep(final @NonNull UUID runId) {
        return getParentRunId(runId)
                .flatMap(parentRunIdOptional -> parentRunIdOptional
                        .map(this::getWebhookStep)
                        .orElseGet(() -> Either.left(RejectMessage.of500("Could not get parent webhook step."))));
    }

    @Override
    public Either<RejectMessage, List<omegapoint.opera.operationaljournal.domain.model.table.JournalErrorItem>> getRunsWithExhaustedRetriesFromLast24Hours() {
        logger.info("Entering getRunsWithExhaustedRetriesFromLast24Hours");
        try (Connection connection = dataSource.getConnection()) {
            String insertSql = """
                                SELECT JournalID, run.RunID, Attempt, WebhookStep, OriginFunction, [Message], [Status], [Timestamp], run.ParentID
                                FROM OperationsJournal.MostRecentJournal mrj
                                INNER JOIN OperationsJournal.Run run ON run.RunID = mrj.RunID
                                INNER JOIN OperationsJournal.QueueCheckpoint qc ON qc.RunID = run.RunID
                                WHERE 
                                    (Status = 'ERROR' 
                                    OR (Status IN ('STARTED', 'RESTARTED') AND DATEDIFF(minute, Timestamp, SYSUTCDATETIME()) > 10))
                                    AND 
                                    Attempt = 6
                                    AND 
                                    [Timestamp] >= DATEADD(hour, -48, SYSUTCDATETIME())                                                                                                        
                                ORDER BY [Timestamp] DESC;
                            """;
            PreparedStatement statement = connection.prepareStatement(insertSql);
            ResultSet resultSet = statement.executeQuery();
            List<JournalErrorItem> journalItems = new ArrayList<>();
            while (resultSet.next()) {
                    journalItems.add(new JournalErrorItem(
                            new RunId(resultSet.getString("RunID")),
                            new Attempt(resultSet.getInt("Attempt")),
                            new TimeStamp(resultSet.getString("Timestamp")),
                            new Status(resultSet.getString("Status")),
                            new Message(resultSet.getString("Message")),
                            new OriginFunction(resultSet.getString("OriginFunction")),
                            new WebhookStep(resultSet.getString("WebhookStep")),
                            new RunId( resultSet.getString("ParentID"))
                    ));
            }

            return Either.right(journalItems.stream().map(JournalErrorItem::toDomain).toList());
        } catch (SQLException e) {
            return Either.left(RejectMessage.of500("Failed to check for existing Journal rows. DatabaseRepository: " + e.getMessage()));
        }
    }
}
