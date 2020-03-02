package com.plugin.brightcove;

import android.content.Context;
import android.content.Intent;
import android.util.Log;


import androidx.annotation.NonNull;

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

public class BrightcovePlayer extends CordovaPlugin {
    private CallbackContext msgBusEventCallback = null;
    private CallbackContext checkIfPlayerClosedCallback = null;


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        //register EventBus (this activity is a subscriber)
        EventBus.getDefault().register(this);

    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

        if (action.equals("createPlayer")) {

            JSONObject brightcovePlayerArgs = args.getJSONObject(0);
            Context context = cordova.getActivity().getApplicationContext();
            Intent intent = new Intent(context, BrightcoveMainActivity.class);
            /*intent.putExtra("embed_code", (String) ooyalaPlayerArgs.get("embed_code"));
            intent.putExtra("pcode", (String) ooyalaPlayerArgs.get("pcode"));
            intent.putExtra("domain", (String) ooyalaPlayerArgs.get("domain"));
            intent.putExtra("playerTitle", (String) ooyalaPlayerArgs.get("player_title"));
            intent.putExtra("live", (Boolean) ooyalaPlayerArgs.get("live"));*/
            cordova.getActivity().startActivity(intent);

            callbackContext.success("[player create] success");
        }



        return false;

    }

    // This method will be called when a OoyalaPlayerEvent is posted
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBrightcovePlayerEvent(@NonNull BrightcovePlayerEvent event) throws JSONException {
        if (event.message.equals("player_error")) {
            sendCallbackErrorEvent(event.eventArgs);
        } else {
            if (event.eventArgs != null) sendCallbackEvent(event.eventArgs);
        }

    }


    private void sendCallbackErrorEvent(JSONObject jsonObject) throws JSONException {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, jsonObject.getString("errorMessage"));
        pluginResult.setKeepCallback(true);
        msgBusEventCallback.sendPluginResult(pluginResult);
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

    }


    @Override
    public void onDestroy() {
        //unregister eventbus here
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }


}