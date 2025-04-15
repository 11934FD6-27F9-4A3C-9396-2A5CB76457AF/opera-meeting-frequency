package meeting.frequency.parameter;

import java.util.Arrays;

public enum Office{
    BASALT("Basalt"),
    GOTHENBURG("Göteborg"),
    MALMO("Malmö"),
    OREBRO("Örebro"),
    OSLO("Oslo"),
    STOCKHOLM("Stockholm"),
    UMEA("Umeå"),
    UPPSALA("Uppsala");

    public final String rawName;

    Office(final String name) {this.rawName = name;}

    public static Office convertToOffice(final String name){
        return Arrays.stream(Office.values())
                .filter(office -> office.rawName.equals(name))
                .findAny()
                .orElseThrow(() -> new RuntimeException("could not find name of office : " + name));
    }

    public String getRawName(){
        return rawName;
    }
}
