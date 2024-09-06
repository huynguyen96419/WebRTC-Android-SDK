package io.antmedia.webrtcandroidframework.core;

import android.media.projection.MediaProjection;
import android.util.Log;

public abstract class CustomMediaProjectionCallback extends MediaProjection.Callback {

    private static final String TAG = "CustomMediaProjectionCallback";

    public CustomMediaProjectionCallback() {
        super();
    }

    public abstract void onMediaProjection(MediaProjection mediaProjection);

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onCapturedContentResize(int width, int height) {
        super.onCapturedContentResize(width, height);
    }

    @Override
    public void onCapturedContentVisibilityChanged(boolean isVisible) {
        super.onCapturedContentVisibilityChanged(isVisible);
    }
}