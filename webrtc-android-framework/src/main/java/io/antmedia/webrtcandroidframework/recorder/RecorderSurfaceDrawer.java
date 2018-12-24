package io.antmedia.webrtcandroidframework.recorder;

import android.graphics.Matrix;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaMuxer;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import org.webrtc.EglBase;
import org.webrtc.EglBase14;
import org.webrtc.GlRectDrawer;
import org.webrtc.Logging;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoFrame;
import org.webrtc.VideoFrameDrawer;

import java.io.File;
import java.io.IOException;

import io.antmedia.webrtcandroidframework.recorder.VideoEncoderCore;


public class RecorderSurfaceDrawer extends Handler {


    public static final int MSG_FRAME_AVAILABLE = 1;
    private static final int MSG_RELEASE = 2;
    private static final int MSG_START_RECORDING = 3;
    private static final int MSG_STOP_RECORDING = 4;

    private final EglBase eglBase;
    private final File file;
    private final int videoBitrate;
    private Surface textureInputSurface;

    private final GlRectDrawer textureDrawer = new GlRectDrawer();
    private final VideoFrameDrawer videoFrameDrawer = new VideoFrameDrawer();

    public static final String TAG = "RecorderSurfaceDrawer";

    public final static int SAMPLE_AUDIO_RATE_IN_HZ = 48000;

    @Nullable
    private EglBase14 textureEglBase;
    private ThreadUtils.ThreadChecker encodeThreadChecker = new ThreadUtils.ThreadChecker();
    private VideoEncoderCore mVideoEncoder;
    private MediaMuxer mMuxer;
    private AudioRecorderThread audioThread;
    private boolean isStarted = false;
    private int frameCount = 0;


    public RecorderSurfaceDrawer(EglBase eglBase, Surface surface, Looper looper, int videoBitrate, File file) {
        super(looper);
        this.eglBase = eglBase;
        this.textureInputSurface = surface;
        this.file = file;
        this.videoBitrate = videoBitrate;
    }

    public void startRecording(int width, int height) {
        sendMessage(obtainMessage(MSG_START_RECORDING, width, height));
    }

    public void stopRecording() {
        sendMessage(obtainMessage(MSG_STOP_RECORDING));
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void handleMessage(Message inputMessage) {
        int what = inputMessage.what;
        //Log.d(TAG, "CameraHandler [" + this + "]: what=" + what);


        switch (what) {
            case MSG_START_RECORDING:

                try {

                    // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
                    // because our MediaFormat doesn't have the Magic Goodies.  These can only be
                    // obtained from the encoder after it has started processing data.
                    //
                    // We're not actually interested in multiplexing audio.  We just want to convert
                    // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
                    mMuxer = new MediaMuxer(file.toString(),
                            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

                    {
                        //video encoder block
                        mVideoEncoder = new VideoEncoderCore(inputMessage.arg1, inputMessage.arg2, videoBitrate, mMuxer);
                        this.textureEglBase = new EglBase14((EglBase14.Context) eglBase.getEglBaseContext(), EglBase.CONFIG_RECORDABLE);
                        this.textureEglBase.createSurface(mVideoEncoder.getInputSurface());

                        Log.i("RecorderSurfaceDrawer", "init surface size: " + this.textureEglBase.surfaceWidth() + "x" + this.textureEglBase.surfaceHeight());

                        this.textureEglBase.makeCurrent();
                        this.encodeThreadChecker.detachThread();
                    }

                    {
                        //audio encoder block

                        audioThread = new AudioRecorderThread(SAMPLE_AUDIO_RATE_IN_HZ, System.currentTimeMillis(), mMuxer);
                        audioThread.start();
                    }



                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;
            case MSG_STOP_RECORDING:
                mVideoEncoder.drainEncoder(true);
                audioThread.stopAudioRecording();

                while (audioThread.isAlive()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                mMuxer.stop();
                mMuxer.release();

                break;
            case  MSG_FRAME_AVAILABLE:
                if (isStarted || mVideoEncoder.getTrackIndex() == -1) {
                    mVideoEncoder.drainEncoder(false);
                    _drawTextureBuffer((VideoFrame) inputMessage.obj);
                }
                if (!isStarted && mVideoEncoder.getTrackIndex() != -1 && audioThread.getTrackIndex() != -1) {
                    isStarted = true;
                    mMuxer.start();
                    mVideoEncoder.setMuxerStarted(true);
                    audioThread.setMuxerStarted(true);
                }
                frameCount++;

                break;
            case MSG_RELEASE:
                _release();
                break;
            default:
                throw new RuntimeException("unknown msg " + what);
        }
    }



    public void drawTextureBuffer(VideoFrame videoFrame) {
        sendMessage(obtainMessage(
                MSG_FRAME_AVAILABLE, videoFrame));

    }

    private void _drawTextureBuffer(VideoFrame videoFrame) {
        this.encodeThreadChecker.checkIsOnValidThread();

        try {

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            VideoFrame derotatedFrame = new VideoFrame(videoFrame.getBuffer(), 0, videoFrame.getTimestampNs());
            this.videoFrameDrawer.drawFrame(derotatedFrame, this.textureDrawer, (Matrix)null);
            derotatedFrame.release();
            videoFrame.release();
            this.textureEglBase.swapBuffers(videoFrame.getTimestampNs());
            Log.v("RecorderSurfaceDrawer", "surface width: " + this.textureEglBase.surfaceWidth() +
                    "height: " + this.textureEglBase.surfaceHeight());

        } catch (RuntimeException var3) {
            Logging.e("HardwareVideoEncoder", "encodeTexture failed", var3);
        }

    }

    public void release(){
        sendMessage(obtainMessage(MSG_RELEASE));
    }

    private void _release() {

        mVideoEncoder.release();

        this.textureDrawer.release();
        this.videoFrameDrawer.release();
        if (this.textureEglBase != null) {
            this.textureEglBase.release();
            this.textureEglBase = null;
        }

        if (this.textureInputSurface != null) {
            this.textureInputSurface.release();
            this.textureInputSurface = null;
        }
    }


}
