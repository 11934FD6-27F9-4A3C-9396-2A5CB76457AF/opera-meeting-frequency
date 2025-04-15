package meeting.frequency.service.fetch;

import meeting.frequency.parameter.Office;
import meeting.frequency.parameter.ParameterService;
import meeting.frequency.secret.SecretService;
import meeting.frequency.service.fetch.model.Message;
import meeting.frequency.service.integration.slack.SlackHttpClient;
import meeting.frequency.service.integration.slack.pojo.MessagesItem;
import meeting.frequency.service.integration.slack.pojo.SlackHistoryResponse;
import meeting.frequency.service.integration.slack.pojo.user.SlackUserInfoResponse;
import meeting.frequency.service.integration.slack.pojo.user.User;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FetchSlackMessages implements FetchMessageService{

    private final SlackHttpClient slackHttpClient;
    private final static int DAYS_TO_READ = 7;
    private final Map<String, Office> nameCorrespondingOffice;


    public FetchSlackMessages(final SecretService secretService, final ParameterService parameterService) {

        this.slackHttpClient = new SlackHttpClient(secretService);
        this.nameCorrespondingOffice = parameterService.personToOfficeMapping();

    }

    public FetchSlackMessages(final SlackHttpClient slackHttpClient,
                              final Map<String, Office> nameCorrespondingOffice) {

        this.slackHttpClient = slackHttpClient;
        this.nameCorrespondingOffice = nameCorrespondingOffice;
    }

    @Override
    public List<Message> fetchMessages(){

        final SlackHistoryResponse slackHistoryResponse = slackHttpClient.fetchSlackHistory(ZonedDateTime.now(ZoneId.of("Europe/Stockholm")).minusDays(DAYS_TO_READ).toEpochSecond());

        final Map<String, List<MessagesItem>> userIdToMessagesMap = slackHistoryResponse.messages()
                .stream()
                .filter(messagesItem -> messagesItem.blocks() != null) //To filter out messages like "xxx have joined the channel"
                .collect(Collectors.groupingBy(MessagesItem::user));

        final Map<String, List<MessagesItem>> nameToMessagesMap = processUsersConcurrently(userIdToMessagesMap.keySet())
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getValue, idToNameMap -> userIdToMessagesMap.get(idToNameMap.getKey())));

        return nameToMessagesMap.entrySet()
                .stream()
                .map(this::processMessage)
                .toList();
    }

    private Message processMessage(final Map.Entry<String, List<MessagesItem>> nameToMessages) {


        if(!nameCorrespondingOffice.containsKey(nameToMessages.getKey())){
            System.out.printf("Could not match %s to any office%n", nameToMessages.getKey());
        }

        return new Message(nameToMessages.getKey(),
                nameCorrespondingOffice.getOrDefault(nameToMessages.getKey(), Office.STOCKHOLM).getRawName(),
                extractRawMessages(nameToMessages.getValue()));
    }

    private List<String> extractRawMessages(final List<MessagesItem> messagesItems) {
        return messagesItems.stream()
                .map(MessagesItem::text)
                .toList();
    }

    private Map<String, String> processUsersConcurrently(final Set<String> inputSet) {

            return inputSet.stream()
                    .map(slackHttpClient::fetchUserInformation)
                    .map(SlackUserInfoResponse::user)
                    .filter(user -> !user.equals(User.EMPTY_USER()))
                    .collect(Collectors.toMap(User::id, User::realName));
        }
}

    /*
    private Map<String, String> processUsersConcurrently(final Set<String> inputSet) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<SlackUserInfoResponse>> futures = inputSet.stream()
                    .map(userId -> CompletableFuture.supplyAsync(() -> slackHttpClient.fetchUserInformation(userId), executor))
                    .toList();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .map(SlackUserInfoResponse::user)
                    .filter(user -> !user.equals(User.EMPTY_USER()))
                    .collect(Collectors.toMap(User::id, User::realName));
        }
    }

     */
