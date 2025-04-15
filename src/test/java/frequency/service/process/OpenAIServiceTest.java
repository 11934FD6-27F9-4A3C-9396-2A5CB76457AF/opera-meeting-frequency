package frequency.service.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.services.blocking.ChatService;
import com.openai.services.blocking.chat.ChatCompletionService;
import meeting.frequency.service.fetch.model.Message;
import meeting.frequency.service.process.OpenAIService;
import meeting.frequency.service.process.model.MeetingFrequency;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAIServiceTest {

    private final OpenAIClient openAIClient = mock(OpenAIClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenAIService openAIService = new OpenAIService(openAIClient, objectMapper);

    @Test
    public void should_handle_good_response_fromOpenAI() throws IOException, URISyntaxException {

        givenOpenAIReturnsValidResponse();

        final List<MeetingFrequency> result = openAIService.process(List.of(new Message("Test testsson",
                "Stockholm", List.of("1 möte OP"))));

        assertEquals(List.of(new MeetingFrequency("Test testsson", 1, List.of("OP"), "")), result);
    }

    private void givenOpenAIReturnsValidResponse() {

        final ChatCompletion chatCompletion = ChatCompletion.builder()
                .choices(List.of(ChatCompletion.Choice.builder()
                        .message(ChatCompletionMessage.builder()
                                .refusal("no")
                                .content("""
                                        {
                                            "meetingFrequencyItems": [
                                                {
                                                    "name": "Test testsson",
                                                    "meetings": 1,
                                                    "companies": [
                                                        "OP"
                                                    ],
                                                    "office": ""
                                                }
                                            ]
                                        }
                                        """)
                                .build())
                        .finishReason(ChatCompletion.Choice.FinishReason.LENGTH)
                        .index(1)
                        .logprobs(ChatCompletion.Choice.Logprobs.builder().content(List.of()).refusal(List.of()).build())
                        .build()))
                .id("1")
                .created(100)
                .model("GTP4")
                .build();

        ChatService chatService = mock(ChatService.class);
        ChatCompletionService chatCompletionService = mock(ChatCompletionService.class);
        when(openAIClient.chat()).thenReturn(chatService);
        when(chatService.completions()).thenReturn(chatCompletionService);
        when(chatCompletionService.create(any())).thenReturn(chatCompletion);
    }

}