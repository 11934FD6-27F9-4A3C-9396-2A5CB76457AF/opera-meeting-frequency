package omegapoint.opera.operationaljournal.infrastructure.model;

import omegapoint.opera.operationaljournal.domain.model.table.JournalErrorItem;

import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

public class AlertMessages {
        public final List<String> messages;
        private static final Map<String, Integer> replacementCounts = new HashMap<>();

        private AlertMessages(final List<String> messages) {
            this.messages = notNull(messages);
        }

    public static AlertMessages fromDomain(final List<JournalErrorItem> journalErrorItems) {
        Map<String, String> alertMessageMap = new HashMap<>();

        for (JournalErrorItem journalErrorItem : journalErrorItems) {
            String webhookStep = journalErrorItem.webhookStep;

            int replacementCount = replacementCounts.getOrDefault(webhookStep, 0) + 1;
            replacementCounts.put(webhookStep, replacementCount);

            String formattedMessage = formatJournalErrorItems(journalErrorItem, replacementCount);

            alertMessageMap.put(webhookStep, formattedMessage);
        }

        return new AlertMessages(new ArrayList<>(alertMessageMap.values()));
    }

        private static String formatJournalErrorItems(final JournalErrorItem journalErrorItem, final int webhookCount) {
            return String.format(
                    "Journal Error Alert%n" +
                            "Parent ID: %s%n" +
                            "Run ID: %s%n" +
                            "Timestamp: %s%n" +
                            "Status: %s%n" +
                            "Message: %s%n" +
                            "Origin Function: %s%n" +
                            "Webhook Step: %s%n" +
                            "Number of webhook fails: %d",
                    journalErrorItem.parentID,
                    journalErrorItem.runID,
                    journalErrorItem.timestamp,
                    journalErrorItem.status,
                    journalErrorItem.message,
                    journalErrorItem.originFunction,
                    journalErrorItem.webhookStep,
                    webhookCount
                    );
        }
    }




