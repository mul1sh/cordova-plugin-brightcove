package com.plugin.brightcove;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.widget.LinearLayout;
import android.widget.MediaController;

import com.brightcove.player.appcompat.BrightcovePlayerActivity;
import com.brightcove.player.edge.Catalog;
import com.brightcove.player.edge.VideoListener;
import com.brightcove.player.event.EventEmitter;
import com.brightcove.player.event.EventType;
import com.brightcove.player.mediacontroller.BrightcoveMediaController;
import com.brightcove.player.model.Video;
import com.brightcove.player.view.BrightcoveVideoView;
import com.brightcove.cast.GoogleCastComponent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import org.json.JSONException;
import org.json.JSONObject;

import static java.util.Objects.requireNonNull;

public class BrightcovePlayerMainActivity extends BrightcovePlayerActivity {

    private Handler handler = new Handler();
    private Runnable runnable = () -> {
        // hide the header
        requireNonNull(getSupportActionBar()).hide();
        // hide the controls
        baseVideoView.getBrightcoveMediaController().hide();
    };

    private BrightcoveMediaController mediaController = null;

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // register EventBus (this activity is a subscriber)
        EventBus.getDefault().register(this);

        // set the content layout
        setContentView(getResId(this, "layout", "player_layout"));

        // Perform the internal wiring to be able to make use of the BrightcovePlayerFragment.
        baseVideoView = (BrightcoveVideoView) findViewById(getResId(this, "id", "brightcove_video_view"));

        // init the player layout
        final LinearLayout playerLayout = findViewById(getResId(this, "id", "player_layout"));

        // get video params
        Bundle extras = getIntent().getExtras();
        String videoId = extras != null ? extras.getString("videoId") : "";
        String accountId = extras != null ? extras.getString("accountId") : "";
        String policyId = extras != null ? extras.getString("policyId") : "";
        String playerTitle = extras != null ? extras.getString("playerTitle") : "";

        Catalog catalog = new Catalog(baseVideoView.getEventEmitter(), accountId, policyId);

        catalog.findVideoByID(requireNonNull(videoId), new VideoListener() {
            @Override
            public void onVideo(Video video) {
                String title = video.getName();
                if (!TextUtils.isEmpty(title)) {
                    requireNonNull(getSupportActionBar()).setTitle(title);
                } else {
                    requireNonNull(getSupportActionBar()).setTitle(playerTitle);
                }
                baseVideoView.add(video);
                baseVideoView.getEventEmitter().emit(EventType.ENTER_FULL_SCREEN);
            }
        });

        EventEmitter eventEmitter = baseVideoView.getEventEmitter();

        eventEmitter.on(EventType.DID_REMOVE_VIDEO_STILL, event -> {
             // reset the media controller
             baseVideoView.setMediaController(mediaController);
             // hide the player controls
             hideActionBar();
        });


        eventEmitter.on(EventType.ENTER_FULL_SCREEN, event -> {

            mediaController = baseVideoView.getBrightcoveMediaController();
            baseVideoView.setMediaController((MediaController)null);

            // listen for touch events
            playerLayout.setOnTouchListener((view, motionEvent) -> {
                if(!requireNonNull(getSupportActionBar()).isShowing()){
                    getSupportActionBar().show();
                    baseVideoView.getBrightcoveMediaController().show();
                }
                else hideActionBar();
                return true;
            });

            //start playing the video
            baseVideoView.start();
        });

        GoogleCastComponent googleCastComponent = new GoogleCastComponent(eventEmitter, this);
        //You can check if there is a session available
        googleCastComponent.isSessionAvailable();
        googleCastComponent.setAutoPlay(true);
    }

    private void hideActionBar(){ handler.postDelayed(runnable, 3000); }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        GoogleCastComponent.setUpMediaRouteButton(this, menu);
        return true;
    }

    /**
     * Return a resource identifier for the given resource name.
     *
     * @param context The applications context.
     * @param type    Resource type to find (id or layout or string ...)
     * @param name    The name of the desired resource.
     * @return The associated resource identifier or 0 if not found.
     */
    private int getResId(@NonNull Context context, String type, String name) {
        Resources res = context.getResources();
        String pkgName = context.getPackageName();
        int resId = res.getIdentifier(name, type, pkgName);
        return resId == 0 ? Resources.getSystem().getIdentifier(name, type, "android") : resId;
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();

        // send a notification that the player has been closed
        String eventResult = "{'result':'player_has_been_closed'}";
        JSONObject args = null;
        try {
            args = new JSONObject(eventResult);
        } catch (JSONException ignored) {

        }

        EventBus.getDefault().post(new BrightcovePlayerEvent("player_closed_event", args));
        //unregister eventbus here
        EventBus.getDefault().unregister(this);

    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(runnable);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (baseVideoView != null) {
            baseVideoView.stopPlayback();
        }
        // kill the activity on back button press
        finish();
    }

    // This method will be called when a BrightcovePlayerEvent is posted
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void BrightcovePlayerEvent(@NonNull BrightcovePlayerEvent event) throws JSONException {

        // player functionality
        if (event.message.equals("pause_player")) {
            if (baseVideoView != null) {
                baseVideoView.pause();
                handlePlayerEvents(event, event.message + ", success ");
            } else {
                JSONObject eventArgs = new JSONObject("{'errorMessage':'player is null'}");
                EventBus.getDefault().post(new BrightcovePlayerEvent("player_error", eventArgs));
            }
        }

        if (event.message.equals("resume_player")) {
            if (baseVideoView != null) {
                baseVideoView.start();
                handlePlayerEvents(event, event.message + ", success ");
            } else {
                JSONObject eventArgs = new JSONObject("{'errorMessage':'player is null'}");
                EventBus.getDefault().post(new BrightcovePlayerEvent("player_error", eventArgs));
            }
        }

        if (event.message.equals("stop_player")) {
            if (baseVideoView != null) {
                baseVideoView.stopPlayback();
                handlePlayerEvents(event, event.message + ", success ");
            } else {
                JSONObject eventArgs = new JSONObject("{'errorMessage':'player is null'}");
                EventBus.getDefault().post(new BrightcovePlayerEvent("player_error", eventArgs));
            }
        }


    }

    public void handlePlayerEvents(@NonNull BrightcovePlayerEvent event, Object result) throws JSONException {
        String eventResult = "{'result':'" + result + "'}";
        Log.v("event result", eventResult);
        JSONObject args = new JSONObject(eventResult);
        String playerTag = "brightcove_player_";
        EventBus.getDefault().post(new BrightcovePlayerEvent(playerTag + event.message, args));
    }

}
