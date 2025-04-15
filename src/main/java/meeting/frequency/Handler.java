package meeting.frequency;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import meeting.frequency.parameter.ParameterService;
import meeting.frequency.parameter.ParameterStoreService;
import meeting.frequency.secret.SecretManagerService;
import meeting.frequency.secret.SecretService;
import meeting.frequency.service.fetch.FetchMessageService;
import meeting.frequency.service.fetch.FetchSlackMessages;
import meeting.frequency.service.fetch.model.Message;
import meeting.frequency.service.generate.ExcelDocumentService;
import meeting.frequency.service.generate.GenerateDocumentService;
import meeting.frequency.service.process.OpenAIService;
import meeting.frequency.service.process.ProcessMessageService;
import meeting.frequency.service.process.model.MeetingFrequency;
import meeting.frequency.service.upload.SlackUploadService;
import meeting.frequency.service.upload.UploadService;

import java.io.File;
import java.util.Comparator;
import java.util.List;

public class Handler implements RequestHandler<Void, String> {

    private final FetchMessageService fetchMessageService;
    private final ProcessMessageService processMessageService;
    private final GenerateDocumentService generateDocumentService;
    private final UploadService uploadService;

    public Handler(final FetchMessageService fetchMessageService, final ProcessMessageService processMessageService,
                   final GenerateDocumentService generateDocumentService, final UploadService uploadService) {

        this.fetchMessageService = fetchMessageService;
        this.processMessageService = processMessageService;
        this.generateDocumentService = generateDocumentService;
        this.uploadService = uploadService;
    }

    public Handler() {

        SecretService secretService = new SecretManagerService();
        ParameterService parameterService = new ParameterStoreService();

        this.fetchMessageService = new FetchSlackMessages(secretService, parameterService);
        this.processMessageService = new OpenAIService(secretService);
        this.generateDocumentService = new ExcelDocumentService();
        this.uploadService = new SlackUploadService(secretService);
    }


    @Override
    public String handleRequest(final Void unused, final Context context) {

        try {

            System.out.println("Fetching messages...");
            final List<Message> messages = fetchMessageService.fetchMessages();

            if(messages.isEmpty()){
                return "No messages found";
            }

            System.out.println("Process messages...");
            final List<MeetingFrequency> meetingFrequency = processMessageService.process(messages)
                    .stream()
                    .filter(frequency -> frequency.meetings() > 0)
                    .sorted(Comparator.comparingInt(MeetingFrequency::meetings).reversed())
                    .toList();

            System.out.println("Generate file...");
            File file = generateDocumentService.generateDocument(meetingFrequency);

            System.out.println("Upload file...");
            final boolean uploadSuccess = uploadService.upload(file);

            if (uploadSuccess){
                System.out.println("Successfully uploaded file : " + file);
            } else {
                System.out.println("Failed uploaded file : " + file);
                throw new RuntimeException("Failed to upload file");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }


        return "Success";
    }
}