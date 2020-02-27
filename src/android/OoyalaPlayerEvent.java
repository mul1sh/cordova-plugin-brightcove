package cordova_plugin_ooyala;

import org.json.JSONObject;

public class OoyalaPlayerEvent {
    public String message;
    public JSONObject eventArgs;


    public OoyalaPlayerEvent(String message, JSONObject eventArgs) {
        this.message = message;
        this.eventArgs = eventArgs;
    }


}
