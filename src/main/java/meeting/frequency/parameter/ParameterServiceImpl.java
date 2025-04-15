package meeting.frequency.parameter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ParameterServiceImpl implements ParameterService{

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Office> personToOfficeMapping() {

        try {
            //Returns office as key and list of names of people belonging to that office
            final Map<String, List<String>> officeToNamesMapping = objectMapper.readValue(listOfPeopleInOffices, new TypeReference<>() {});

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

    //Todo read from Azure?
    String listOfPeopleInOffices = """
    {
                      "Basalt": [
                        "Caroline Lindholm",
                        "Johan Reinholdz",
                        "Lilian Westin"
                      ],
                      "Göteborg": [
                        "Peo Strand",
                        "Alicia Fylke",
                        "Johan Gärdt",
                        "Ingela Samuelsson",
                        "Anna Storm",
                        "Pelle Andel",
                        "Dennis Ahlborg",
                        "Annica Lörstad",
                        "Carin Hellberg",
                        "Mikael Pielage"
                      ],
                      "Malmö": [
                        "Kim Wikbrand",
                        "Henrik Friström",
                        "Johan Stam",
                        "Nils Hall",
                        "Emil Andersson",
                        "Jens Ode",
                        "Robert Svensson",
                        "Sandra Mikaelson",
                        "Sebastian Lundh",
                        "Thomas Engström"
                      ],
                      "Örebro": [
                        "Stefan Reinebo",
                        "Magnus Ahlgren",
                        "Patrik Lundin",
                        "Rikard Dahl",
                        "Sebastian Rimsh",
                        "Douglas Ingman",
                        "Håkan Sjöström",
                        "Agneta Lindqvist",
                        "Daniel Lennartsson"
                      ],
                      "Oslo": [
                        "Anja Bergby",
                        "Praveen Kirubaharan",
                        "Tom Henrik Rogstad",
                        "Kjetil Haugen",
                        "Anders Kofoed",
                        "Anders Riise Mæhlum",
                        "Erik Haug",
                        "Christian Tingvoll",
                        "Lars Ulslev Johannessen",
                        "Marius Thingwall",
                        "Sindre Schei",
                        "Oscar Conrad"
                      ],
                      "Stockholm": [
                        "Anders Lindberg",
                        "Bobby Singh",
                        "Ola Rodin",
                        "Jonas Rehn",
                        "Karin Bommelin",
                        "Anders Skedeby",
                        "Anders Fristedt",
                        "Andreas Leander",
                        "Anna-Clara Söderbaum",
                        "Anton Sandberg",
                        "Caroline Forssblad",
                        "Dan Bryntze",
                        "Daniel Deogun",
                        "Emil Rimling",
                        "Henrik Johansson",
                        "Henrik Kraft",
                        "Joakim Borell",
                        "Johan Appelquist",
                        "Larry Björkqvist",
                        "Marie Lönn",
                        "Mats Persson",
                        "Mikael Nordelind",
                        "Nada Kapidzic Cicovic",
                        "Nils Ågren",
                        "Olle Mulmo",
                        "Samir Dzaferagic",
                        "Tim Sönderskov",
                        "Ulrika",
                        "Wilhelm Hedman-Dybeck"
                      ],
                      "Umeå": [
                        "Mattias Sällström",
                        "Markus Örebrand",
                        "Linus Lagerhjelm"
                      ],
                      "Uppsala": [
                        "Patrik Lannergård",
                        "Fredrik Metsänoja",
                        "Christina Bellman",
                        "Henrik Heijkenskjold",
                        "Jonas Åberg",
                        "Lars Secher"
                      ]
                    }
    """;
}
