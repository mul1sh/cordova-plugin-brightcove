package cordova_plugin_ooyala;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Observable;
import java.util.Observer;

// ooyala libs
import com.ooyala.cast.CastManager;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.ooyala.android.OoyalaNotification;
import com.ooyala.android.OoyalaPlayer;
import com.ooyala.android.OoyalaPlayerLayout;
import com.ooyala.android.PlayerDomain;
import com.ooyala.android.configuration.Options;
import com.ooyala.android.ui.OoyalaPlayerLayoutController;


public class OoyalaPlayerActivity extends AppCompatActivity implements Observer {

    private OoyalaPlayer player;
    private String TAG = this.getClass().toString();
    private CastManager castManager;
    private CastViewManager castViewManager;
    private OoyalaPlayerLayoutController playerLayoutController;

    /**
     * Called when the activity is first created.
     */

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResId(this, "layout", "player_simple_layout"));
        // register EventBus (this activity is a subscriber)
        EventBus.getDefault().register(this);

        // set the cast manager
        castManager = CastManager.getCastManager();


        // get the settings
        Bundle extras = getIntent().getExtras();
        String embedCode = extras.getString("embed_code");
        String pcode = extras.getString("pcode");
        String domain = extras.getString("domain");
        String playerTitle = extras.getString("playerTitle");

        //set the player title
        getSupportActionBar().setTitle(playerTitle);

        // Initialize Ooyala Player
        OoyalaPlayerLayout playerLayout = findViewById(getResId(this, "id", "ooyalaPlayer"));
        PlayerDomain playerDomain = new PlayerDomain(domain);
        Options options =  new Options.Builder().setUseExoPlayer(true).build();
        

        player = new OoyalaPlayer(pcode, playerDomain,  options);
        playerLayoutController = new OoyalaPlayerLayoutController(playerLayout, player);


        //Create a CastManager, and connect to the OoyalaPlayer
        castManager.registerWithOoyalaPlayer(playerLayoutController.getPlayer());

        castViewManager = new CastViewManager(this, castManager);

        player.addObserver(this);

        if (player.setEmbedCode(embedCode)) {
            //Uncomment for Auto Play
            player.play();
        } else {
            Log.e(TAG, "Asset Failure");
        }
         //hide the action bar
        hideActionBar();

        // listen for touch events
        playerLayout.setOnTouchListener(new View.OnTouchListener() {
          @Override
          public boolean onTouch(View v, MotionEvent event){
            if(!getSupportActionBar().isShowing()){
               getSupportActionBar().show();
               playerLayoutController.getControls().show();
            }
            else{
              hideActionBar();
            }
            return true;
          }

        });
    }

    private void hideActionBar(){
        new android.os.Handler().postDelayed(
                new Runnable() {
                  public void run() {
                    // hide the header
                    getSupportActionBar().hide();
                    // hide the controls
                    playerLayoutController.getControls().hide();
                  }
                },
                5000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(getResId(this, "menu", "ooyala_cast_menu"), menu);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, getResId(this, "id", "media_route_menu_item"));
        return true;
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart()");
        super.onStart();
        

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (castManager != null) {
          castManager.deregisterFromOoyalaPlayer();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        if (player != null) {
            player.suspend();
        }

        // send a notification that the player has been closed
        String eventResult = "{'result':'player_has_been_closed'}";
        JSONObject args = null;
        try {
            args = new JSONObject(eventResult);
        } catch (JSONException jse) {

        }

        EventBus.getDefault().post(new OoyalaPlayerEvent("player_closed_event", args));
        //unregister eventbus here
        EventBus.getDefault().unregister(this);


    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        if (castManager != null && player != null) {
           castManager.registerWithOoyalaPlayer(player);
           player.resume();
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        if (player != null) {
          player.suspend();
        }
    }
       /**
     * Listen to all notifications from the OoyalaPlayer
     */
    @Override
    public void update(Observable arg0, Object argN) {
        if (arg0 != player) {
            return;
        }

        final String arg1 = OoyalaNotification.getNameOrUnknown(argN);
        if (arg1 == OoyalaPlayer.TIME_CHANGED_NOTIFICATION_NAME) {
            return;
        }

        if (arg1 == OoyalaPlayer.CURRENT_ITEM_CHANGED_NOTIFICATION_NAME) {

            castViewManager.configureCastView(this, player.getCurrentItem());

        } else if (arg1 == OoyalaPlayer.ERROR_NOTIFICATION_NAME) {
            final String msg = "Error Event Received";
            if (player != null && player.getError() != null) {
                Log.e(TAG, msg, player.getError());
            } else {
                Log.e(TAG, msg);
            }
        }

        if (arg1 == OoyalaPlayer.STATE_CHANGED_NOTIFICATION_NAME) {
            if (player.isInCastMode()) {
                OoyalaPlayer.State state = player.getState();
                castViewManager.updateCastState(this, state);
            }
        }


        if (arg1 == OoyalaPlayer.ERROR_NOTIFICATION_NAME) {
            final String msg = "Error Event Received";
            if (player != null && player.getError() != null) {
                Log.e(TAG, msg, player.getError());
            } else {
                Log.e(TAG, msg);
            }
        }

        if (arg1 == OoyalaPlayer.PLAY_COMPLETED_NOTIFICATION_NAME) {

        }

        // Automation Hook: to write Notifications to a temporary file on the device/emulator
        String text = "Notification Received: " + arg1 + " - state: " + player.getState();


        Log.d(TAG, "Notification Received: " + arg1 + " - state: " + player.getState());
    }


    /**
     * Return a resource identifier for the given resource name.
     *
     * @param context The applications context.
     * @param type    Resource type to find (id or layout or string ...)
     * @param name    The name of the desired resource.
     * @return The associated resource identifier or 0 if not found.
     */
    public int getResId(Context context, String type, String name) {
        Resources res = context.getResources();
        String pkgName = context.getPackageName();

        int resId;
        resId = res.getIdentifier(name, type, pkgName);

        if (resId == 0) {
            resId = Resources.getSystem().getIdentifier(name, type, "android");
        }

        return resId;
    }

    // This method will be called when a OoyalaPlayerEvent is posted
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOoyalaPlayerEvent(OoyalaPlayerEvent event) throws JSONException {

        // player functionality
        if (event.message == "pause_player") {
            if (player != null) {
                player.pause();
                handlePlayerEvents(event, event.message + ", success ");
            } else {
                JSONObject eventArgs = new JSONObject("{'errorMessage':'player is null'}");
                EventBus.getDefault().post(new OoyalaPlayerEvent("player_error", eventArgs));
            }
        }

        if (event.message == "resume_player") {
            if (player != null) {
                if (player.isPlaying() == false) {
                    player.resume();
                }
                handlePlayerEvents(event, event.message + ", success ");
            } else {
                JSONObject eventArgs = new JSONObject("{'errorMessage':'player is null'}");
                EventBus.getDefault().post(new OoyalaPlayerEvent("player_error", eventArgs));
            }
        }
        if (event.message == "suspend_player") {
            if (player != null) {
                player.suspend();
                handlePlayerEvents(event, event.message + ", success ");
            } else {
                JSONObject eventArgs = new JSONObject("{'errorMessage':'player is null'}");
                EventBus.getDefault().post(new OoyalaPlayerEvent("player_error", eventArgs));
            }
        }

        // the gets
        if (event.message == "get_player_playhead_time") {
            if (player != null) {
                int result = player.getPlayheadTime();
                handlePlayerEvents(event, result);
            } else {
                JSONObject eventArgs = new JSONObject("{'errorMessage':'player is null'}");
                EventBus.getDefault().post(new OoyalaPlayerEvent("player_error", eventArgs));
            }
        }
        if (event.message == "get_player_duration") {
            if (player != null) {
                int result = player.getDuration();
                handlePlayerEvents(event, result);
            } else {
                JSONObject eventArgs = new JSONObject("{'errorMessage':'player is null'}");
                EventBus.getDefault().post(new OoyalaPlayerEvent("player_error", eventArgs));
            }
        }
        if (event.message == "get_player_state") {
            if (player != null) {
                OoyalaPlayer.State state = player.getState();
                String result = state.toString();
                handlePlayerEvents(event, result);
            } else {
                JSONObject eventArgs = new JSONObject("{'errorMessage':'player is null'}");
                EventBus.getDefault().post(new OoyalaPlayerEvent("player_error", eventArgs));
            }
        }

        if (event.message == "get_player_bitrate") {
            if (player != null) {
                double result = player.getBitrate();
                handlePlayerEvents(event, result);
            } else {
                JSONObject eventArgs = new JSONObject("{'errorMessage':'player is null'}");
                EventBus.getDefault().post(new OoyalaPlayerEvent("player_error", eventArgs));
            }
        }

        if (event.message == "get_player_is_fullscreen") {
            if (player != null) {
                boolean result = player.isFullscreen();
                handlePlayerEvents(event, result);
            } else {
                JSONObject eventArgs = new JSONObject("{'errorMessage':'player is null'}");
                EventBus.getDefault().post(new OoyalaPlayerEvent("player_error", eventArgs));
            }
        }
        if (event.message == "get_player_is_in_castmode") {
            if (player != null) {
                boolean result = player.isInCastMode();
                handlePlayerEvents(event, result);
            } else {
                JSONObject eventArgs = new JSONObject("{'errorMessage':'player is null'}");
                EventBus.getDefault().post(new OoyalaPlayerEvent("player_error", eventArgs));
            }
        }
        if (event.message == "get_player_is_playing") {
            if (player != null) {
                boolean result = player.isPlaying();
                handlePlayerEvents(event, result);
            } else {
                JSONObject eventArgs = new JSONObject("{'errorMessage':'player is null'}");
                EventBus.getDefault().post(new OoyalaPlayerEvent("player_error", eventArgs));
            }
        }

        // the sets
        if (event.message == "set_player_playhead_time") {
            if (player != null) {
                int result = Integer.parseInt(event.eventArgs.getString("playhead_time"));
                player.setPlayheadTime(result);

                // send back a success msg
                handlePlayerEvents(event, event.message + ", success ");
            } else {
                JSONObject eventArgs = new JSONObject("{'errorMessage':'player is null'}");
                EventBus.getDefault().post(new OoyalaPlayerEvent("player_error", eventArgs));
            }
        }

        if (event.message == "set_player_seekable") {
            if (player != null) {
                boolean result = Boolean.parseBoolean(event.eventArgs.getString("player_seekable"));
                player.setSeekable(result);

                // send back a success msg
                handlePlayerEvents(event, event.message + ", success ");
            } else {
                JSONObject eventArgs = new JSONObject("{'errorMessage':'player is null'}");
                EventBus.getDefault().post(new OoyalaPlayerEvent("player_error", eventArgs));
            }
        }

        if (event.message == "set_player_fullscreen") {
            if (player != null) {
                String result = event.eventArgs.getString("player_set_fullscreen");
                player.setFullscreen(Boolean.parseBoolean(result));

                // send back a success msg
                handlePlayerEvents(event, event.message + ", success ");
            } else {
                JSONObject eventArgs = new JSONObject("{'errorMessage':'player is null'}");
                EventBus.getDefault().post(new OoyalaPlayerEvent("player_error", eventArgs));
            }
        }
    }

    public void handlePlayerEvents(OoyalaPlayerEvent event, Object result) throws JSONException {
        String eventResult = "{'result':'" + (String.valueOf(result)) + "'}";
        Log.v("event result", eventResult);
        JSONObject args = new JSONObject(eventResult);
        EventBus.getDefault().post(new OoyalaPlayerEvent("ooyala_player_" + event.message, args));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (player != null) {
            player.suspend();
        }
        // kill the activity on back button press
        finish();
    }


}
