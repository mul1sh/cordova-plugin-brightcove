package cordova_plugin_ooyala;

import android.content.Context;
import android.content.res.Resources;

import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.media.NotificationOptions;

import java.util.List;


/**
 * It is used to provide google cast options by manifest.
 */
public class CastOptionsProvider implements OptionsProvider {
  private final String APP_ID = "B70158E8";

  @Override
  public CastOptions getCastOptions(Context context) {
    NotificationOptions notificationOptions = new NotificationOptions.Builder()
        .setPlayDrawableResId(getResId(context,"drawable","ic_media_play_light"))
        .setPauseDrawableResId(getResId(context,"drawable","ic_media_pause_light"))
        .build();
    CastMediaOptions mediaOptions = new CastMediaOptions.Builder()
        .setNotificationOptions(notificationOptions)
        .setExpandedControllerActivityClassName(OoyalaPlayerActivity.class.getName())
        .build();

    return new CastOptions.Builder()
        .setReceiverApplicationId(APP_ID)
        .setCastMediaOptions(mediaOptions)
        .build();
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

  @Override
  public List<SessionProvider> getAdditionalSessionProviders(Context context) {
    return null;
  }
}