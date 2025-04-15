package meeting.frequency.service.process;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.ResponseFormatJsonSchema;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import meeting.frequency.secret.SecretService;
import meeting.frequency.service.fetch.model.Message;
import meeting.frequency.service.process.model.MeetingFrequency;
import meeting.frequency.service.process.model.MeetingFrequencyItems;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class OpenAIService implements ProcessMessageService{

    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper;

    public OpenAIService(final SecretService secretService){
        this.openAIClient = OpenAIOkHttpClient.builder()
                .apiKey(secretService.fetchOpenAPISecrets().apiKey())
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public OpenAIService(final OpenAIClient openAIClient, final ObjectMapper objectMapper) {
        this.openAIClient = openAIClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<MeetingFrequency> process(final List<Message> messages) throws IOException, URISyntaxException {

        if(messages.isEmpty()){
            return List.of();
        }

        String messagePrompt = generatePrompt(messages);

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addSystemMessage("You are a string to json parser")
                .addUserMessage(messagePrompt)
                .responseFormat(ChatCompletionCreateParams.ResponseFormat.ofJsonSchema(getJsonSchema()))
                .model(ChatModel.GPT_4O_MINI)
                .build();

        ChatCompletion response = openAIClient.chat().completions().create(params);

        return extractMeetingFrequency(response);
    }

    private List<MeetingFrequency> extractMeetingFrequency(final ChatCompletion response) throws JsonProcessingException {

        return objectMapper.readValue(response.choices().get(0).message().content().get(), MeetingFrequencyItems.class)
                .meetingFrequencyItems();
    }

    private String generatePrompt(final List<Message> messages) {

        final String messageInPrompt = messages.stream()
                .map(message -> message.name() + " -> "
                                + String.join(" + ", message.rawMessages()).replaceAll("\n", "")
                                + " --> " + message.office() +
                                "\n"

                )
                .collect(Collectors.joining());

        //Can probably improve the prompt quite a bit
        return "I have the following string \n"
               + messageInPrompt + "\n" +
               "Where each line is its own array and " +
               "where name usually consists of 2 words and is located left of ->, and meetings field is the sum of all the number that is next to the words \"möte\" and \"möten\" " +
               "or if those words are missing, just any number provided on the line. The companies field is on the strings the right of the -> and are usually seperated by a \",\" or •. or commont in parenthesis" +
               "if it is separated, put it as its own entry in the string array in companies. Attribute office is after the \"-->\" to the right of it att the end before line-break";
    }

    private ResponseFormatJsonSchema getJsonSchema() throws IOException, URISyntaxException {

        ClassLoader classLoader = this.getClass().getClassLoader();
        URL resource = classLoader.getResource("openai/MeetingFrequencyJsonSchema.json");
        Path path = Paths.get(resource.toURI());
        final String contents = Files.readString(path);

        final ResponseFormatJsonSchema.JsonSchema jsonSchema = objectMapper.readValue(contents, ResponseFormatJsonSchema.JsonSchema.class);
        return ResponseFormatJsonSchema.builder().jsonSchema(jsonSchema).build();
    }
}
