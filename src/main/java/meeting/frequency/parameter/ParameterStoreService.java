package meeting.frequency.parameter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ParameterStoreService implements ParameterService{

    private final SsmClient ssmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final static String PARAMETER_NAME = "person.office.mapping";

    public ParameterStoreService(){
        this.ssmClient = SsmClient.builder()
                .region(Region.EU_NORTH_1)  // Replace with your AWS region
                .build();
    }

    @Override
    public Map<String, Office> personToOfficeMapping() {

            GetParameterRequest parameterRequest = GetParameterRequest.builder()
                    .name(PARAMETER_NAME)
                    .withDecryption(true)  // Set to true if the parameter is encrypted
                    .build();

            GetParameterResponse parameterResponse = ssmClient.getParameter(parameterRequest);
            String parameterValue = parameterResponse.parameter().value();


            System.out.println("Fetched Parameter: " + parameterValue);

        try {

            //Returns office as key and list of names of people belonging to that office
            final Map<String, List<String>> officeToNamesMapping = objectMapper.readValue(parameterValue, new TypeReference<>() {});

            return officeToNamesMapping.entrySet()
                    .stream()
                    .flatMap(officeToNamesEntries ->
                            officeToNamesEntries.getValue()
                                    .stream()
                                    .map(name -> Map.entry(name, Office.convertToOffice(officeToNamesEntries.getKey())))
                    )
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
