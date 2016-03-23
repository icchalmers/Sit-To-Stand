package cc.chalmers.sittostand;

import java.util.HashMap;

/**
 * Created by Iain on 04/03/2016.
 */
public class RFDuinoGATTServices {
    private static HashMap<String, String> attributes = new HashMap();
    public static String RFDUINO_SEND_DATA = "00002222-0000-1000-8000-00805f9b34fb";
    public static String RFDUINO_RECEIVE_DATA = "00002221-0000-1000-8000-00805f9b34fb";
    public static String RFDUINO_SERVICE = "00002220-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        attributes.put(RFDUINO_SERVICE, "RFDuino Service");
        attributes.put("00001800-0000-1000-8000-00805f9b34fb", "Device Information Service");
        // Sample Characteristics.
        attributes.put(RFDUINO_SEND_DATA, "RFDuino Send Data");
        attributes.put(RFDUINO_RECEIVE_DATA, "RFDuino Receive Data");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
