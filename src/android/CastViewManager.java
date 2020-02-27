package cordova_plugin_ooyala;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ooyala.android.OoyalaPlayer;
import com.ooyala.cast.CastManager;
import com.ooyala.cast.UpdateImageViewRunnable;
import com.ooyala.android.item.Video;


/**
 * This is an example of a way to manage the view that is displayed in the player while casting
 */
public class CastViewManager {
    private View castView;
    private TextView stateTextView;

    public CastViewManager(Activity activity, CastManager manager) {
        castView = activity.getLayoutInflater().inflate(getResId(activity, "layout", "cast_video_view"), null);
        manager.setCastView(castView);
        stateTextView = (TextView) castView.findViewById(getResId(activity, "id", "castStateTextView"));
    }


    public void configureCastView(Activity activity, Video video) {
        final ImageView castBackgroundImage = (ImageView) castView.findViewById(getResId(activity, "id", "castBackgroundImage"));

        // Update the ImageView on a separate thread
        new Thread(new UpdateImageViewRunnable(castBackgroundImage, video.getPromoImageURL(0, 0))).start();

    }


    public void updateCastState(Context c, OoyalaPlayer.State state) {
        String castDeviceName = CastManager.getCastManager().getDeviceName();
        if (state == OoyalaPlayer.State.LOADING) {
            stateTextView.setText(c.getString(getResId(c, "string", "loading")));
        } else if (state == OoyalaPlayer.State.PLAYING || state == OoyalaPlayer.State.PAUSED) {
            String statusString = String.format(c.getString(getResId(c, "string", "castingTo"), castDeviceName));
            stateTextView.setText(statusString);
        } else {
            stateTextView.setText("");
        }
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
}