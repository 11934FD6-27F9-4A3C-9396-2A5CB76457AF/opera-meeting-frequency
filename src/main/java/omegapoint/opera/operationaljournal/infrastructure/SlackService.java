package omegapoint.opera.operationaljournal.infrastructure;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.Message;
import io.vavr.control.Either;
import lombok.extern.slf4j.Slf4j;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.logging.SuccessMessage;
import omegapoint.opera.operationaljournal.domain.model.table.JournalErrorItem;
import omegapoint.opera.operationaljournal.infrastructure.model.AlertMessages;

import java.util.List;

@Slf4j
public final class SlackService implements omegapoint.opera.operationaljournal.domain.repository.SlackService {
    public final String channelId;
    public final MethodsClient methods;

    public SlackService(final MethodsClient methods, final String channelId) {
        this.methods = methods;
        this.channelId = channelId;
    }

    @Override
    public Either<RejectMessage, SuccessMessage> sendAlert(final List<JournalErrorItem> journalErrorItems) {
        log.info("EXECUTING: Sending a message to Slack!");

        if (journalErrorItems == null || journalErrorItems.isEmpty()) {
            return Either.left(RejectMessage.of("No new JournalErrorItem found"));
        }

        AlertMessages alertMessages = AlertMessages.fromDomain(journalErrorItems);
        for (String alertMessage : alertMessages.messages) {
            ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                    .channel(channelId)
                    .text(alertMessage)
                    .build();
            try {
                ChatPostMessageResponse response = methods.chatPostMessage(request);

                if (response.isOk()) {
                    Message postedMessage = response.getMessage();
                    log.info("Message sent successfully: {}", postedMessage);

                } else {
                    String errorCode = response.getError();
                    log.error("Error while posting chat message to Slack: {}", errorCode);

                    return Either.left(RejectMessage.of("Slack API error: " + errorCode));
                }
            } catch (Exception e) {
                log.error("Exception while sending a message to Slack. @error={}", e.getMessage());

                return Either.left(RejectMessage.of("Exception occurred: " + e.getMessage()));
            }
        }
        SuccessMessage successMessage = SuccessMessage.of("All messages sent successfully!");
        return Either.right(successMessage);
    }
}

