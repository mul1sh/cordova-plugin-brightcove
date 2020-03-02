package com.plugin.brightcove;

import org.json.JSONObject;

class BrightcovePlayerEvent {

    public String message;
    public JSONObject eventArgs;


    public BrightcovePlayerEvent(String message, JSONObject eventArgs) {
        this.message = message;
        this.eventArgs = eventArgs;
    }

}
