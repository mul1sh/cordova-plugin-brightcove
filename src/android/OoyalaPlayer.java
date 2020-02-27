package cordova_plugin_ooyala;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ooyala.cast.CastManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class OoyalaPlayer extends CordovaPlugin {
    private CallbackContext msgBusEventCallback = null;
    private CallbackContext checkIfPlayerClosedCallback = null;


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        //register EventBus (this activity is a subscriber)
        EventBus.getDefault().register(this);
        // init the cast manager
        try {
            CastManager.initialize(cordova.getActivity(), "urn:x-cast:ooyala");

        } catch (CastManager.CastManagerInitializationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

        if (action.equals("createPlayer")) {

            JSONObject ooyalaPlayerArgs = args.getJSONObject(0);
            Context context = cordova.getActivity().getApplicationContext();
            Intent intent = new Intent(context, OoyalaPlayerActivity.class);
            intent.putExtra("embed_code", (String) ooyalaPlayerArgs.get("embed_code"));
            intent.putExtra("pcode", (String) ooyalaPlayerArgs.get("pcode"));
            intent.putExtra("domain", (String) ooyalaPlayerArgs.get("domain"));
            intent.putExtra("playerTitle", (String) ooyalaPlayerArgs.get("player_title"));
            intent.putExtra("live", (Boolean) ooyalaPlayerArgs.get("live"));
            cordova.getActivity().startActivity(intent);

            callbackContext.success("[player create] success");
        }
        if (action.equals("pausePlayer")) {
            EventBus.getDefault().post(new OoyalaPlayerEvent("pause_player", null));
            callbackContext.success("[pause] success");
        }
        if (action.equals("resumePlayer")) {
            EventBus.getDefault().post(new OoyalaPlayerEvent("resume_player", null));
            callbackContext.success("[player resume] success");
        }
        if (action.equals("suspendPlayer")) {
            EventBus.getDefault().post(new OoyalaPlayerEvent("suspend_player", null));
            callbackContext.success("[player suspended] success");
        }
        //gets
        if (action.equals("getPlayerPlayheadTime")) {
            msgBusEventCallback = callbackContext;
            EventBus.getDefault().post(new OoyalaPlayerEvent("get_player_playhead_time", null));
            return true;
        }
        if (action.equals("getPlayerDuration")) {
            msgBusEventCallback = callbackContext;
            EventBus.getDefault().post(new OoyalaPlayerEvent("get_player_duration", null));
            return true;
        }
        if (action.equals("getPlayerState")) {
            msgBusEventCallback = callbackContext;
            EventBus.getDefault().post(new OoyalaPlayerEvent("get_player_state", null));
            return true;
        }
        if (action.equals("getPlayerBitRate")) {
            msgBusEventCallback = callbackContext;
            EventBus.getDefault().post(new OoyalaPlayerEvent("get_player_bitrate", null));
            return true;
        }
        if (action.equals("getPlayerMetaData")) {
            msgBusEventCallback = callbackContext;
            EventBus.getDefault().post(new OoyalaPlayerEvent("get_player_metadata", null));
            return true;
        }
        if (action.equals("isPlayerInFullScreen")) {
            msgBusEventCallback = callbackContext;
            EventBus.getDefault().post(new OoyalaPlayerEvent("get_player_is_fullscreen", null));
            return true;
        }
        if (action.equals("isPlayerInCastMode")) {
            msgBusEventCallback = callbackContext;
            EventBus.getDefault().post(new OoyalaPlayerEvent("get_player_is_in_castmode", null));
            return true;
        }
        if (action.equals("isPlayerPlaying")) {
            msgBusEventCallback = callbackContext;
            EventBus.getDefault().post(new OoyalaPlayerEvent("get_player_is_playing", null));
            return true;
        }
        if (action.equals("isPlayerClosed")) {
            checkIfPlayerClosedCallback = callbackContext;
            return true;
        }
        // the sets
        if (action.equals("setPlayerPlayheadTime")) {
            msgBusEventCallback = callbackContext;
            JSONObject ooyalaPlayerArgs = args.getJSONObject(0);
            String param = ooyalaPlayerArgs.getString("playHeadTime");
            JSONObject eventArgs = new JSONObject("{'playhead_time':'" + param + "'}");
            EventBus.getDefault().post(new OoyalaPlayerEvent("set_player_playhead_time", eventArgs));
            callbackContext.success("[setPlayerPlayheadTime] success");
        }

        if (action.equals("setPlayerSeekable")) {
            msgBusEventCallback = callbackContext;
            JSONObject ooyalaPlayerArgs = args.getJSONObject(0);
            String param = ooyalaPlayerArgs.getString("playerSeekable");
            JSONObject eventArgs = new JSONObject("{'player_seekable':'" + param + "'}");
            EventBus.getDefault().post(new OoyalaPlayerEvent("set_player_seekable", eventArgs));
            callbackContext.success("[setPlayerSeekable] success");
        }


        if (action.equals("setPlayerFullScreen")) {
            msgBusEventCallback = callbackContext;
            JSONObject ooyalaPlayerArgs = args.getJSONObject(0);
            String param = ooyalaPlayerArgs.getString("playerFullScreen");
            JSONObject eventArgs = new JSONObject("{'player_set_fullscreen':'" + param + "'}");
            EventBus.getDefault().post(new OoyalaPlayerEvent("set_player_fullscreen", eventArgs));
            callbackContext.success("[setPlayerFullScreen] success");
        }


        return false;

    }

    // This method will be called when a OoyalaPlayerEvent is posted
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOoyalaPlayerEvent(OoyalaPlayerEvent event) throws JSONException {
        if (event.message == "player_error") {
            sendCallbackErrorEvent(event.eventArgs);
        } else {
            if (event.eventArgs != null) {
                sendCallbackEvent(event.eventArgs);
            }

        }

    }


    private void sendCallbackErrorEvent(JSONObject jsonObject) throws JSONException {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, jsonObject.getString("errorMessage"));
        pluginResult.setKeepCallback(true);
        msgBusEventCallback.sendPluginResult(pluginResult);
        return;
    }

    private void sendCallbackEvent(JSONObject jsonObject) throws JSONException {
        Log.e("json object 1", jsonObject.toString());
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject.getString("result"));

        if (jsonObject.getString("result").equals("player_has_been_closed")) {
            pluginResult.setKeepCallback(false);
            checkIfPlayerClosedCallback.sendPluginResult(pluginResult);
        } else {
            pluginResult.setKeepCallback(true);
            msgBusEventCallback.sendPluginResult(pluginResult);
        }

        return;
    }


    @Override
    public void onDestroy() {
        //unregister eventbus here
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }


}