package io.antmedia.webrtc_android_sample_app.basic;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.webrtc.DataChannel;
import org.webrtc.SurfaceViewRenderer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import io.antmedia.webrtc_android_sample_app.PermissionHandler;
import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;

public class PeerActivity extends TestableActivity {
    private TextView broadcastingView;
    private Button startStreamingButton;
    private String streamId;
    private IWebRTCClient webRTCClient;
    private TextView streamIdEditText;

    boolean bluetoothEnabled = false;
    String serverUrl = "";
    SurfaceViewRenderer fullScreenRenderer;
    SurfaceViewRenderer pipRenderer;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peer);

        fullScreenRenderer = findViewById(R.id.full_screen_renderer);
        pipRenderer = findViewById(R.id.pip_view_renderer);

        broadcastingView = findViewById(R.id.broadcasting_text_view);
        startStreamingButton = findViewById(R.id.start_streaming_button);
        streamIdEditText = findViewById(R.id.stream_id_edittext);

        serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);

        streamIdEditText.setText("streamId");

        if(PermissionHandler.checkCameraPermissions(this)){
            createWebRTCClient();
        }

    }

    public void createWebRTCClient(){
        webRTCClient = IWebRTCClient.builder()
                .setLocalVideoRenderer(pipRenderer)
                .addRemoteVideoRenderer(fullScreenRenderer)
                .setServerUrl(serverUrl)
                .setActivity(this)
                .setBluetoothEnabled(bluetoothEnabled)
                .setWebRTCListener(createWebRTCListener())
                .setDataChannelObserver(createDatachannelObserver())
                .build();

        startStreamingButton = findViewById(R.id.start_streaming_button);
        startStreamingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                streamId = streamIdEditText.getText().toString();
                startStopStream();
            }
        });
    }

    public void startStopStream() {
        if(!PermissionHandler.checkPublishPermissions(this, bluetoothEnabled)) {
            return;
        }

        incrementIdle();
        streamId = streamIdEditText.getText().toString();
        if (!webRTCClient.isStreaming(streamId)) {
            Log.i(getClass().getSimpleName(), "Calling Join");

            webRTCClient.join(streamId);
        }
        else {
            Log.i(getClass().getSimpleName(), "Calling stop");
            webRTCClient.stop(streamId);
        }
    }

    private IDataChannelObserver createDatachannelObserver() {
        return new DefaultDataChannelObserver() {
            @Override
            public void textMessageReceived(String messageText) {
                super.textMessageReceived(messageText);
                Toast.makeText(PeerActivity.this, "Message received: " + messageText, Toast.LENGTH_SHORT).show();
            }
        };
    }

    private IWebRTCListener createWebRTCListener() {
        return new DefaultWebRTCListener() {
            @Override
            public void onWebSocketConnected() {
                super.onWebSocketConnected();
            }

            @Override
            public void onIceConnected(String streamId) {
                super.onIceConnected(streamId);
                startStreamingButton.setText("Stop");
            }

            @Override
            public void onIceDisconnected(String streamId) {
                super.onIceDisconnected(streamId);
                startStreamingButton.setText("Start");

            }

            @Override
            public void onPlayStarted(String streamId) {
                super.onPlayStarted(streamId);
                broadcastingView.setVisibility(View.VISIBLE);
                decrementIdle();
            }

            @Override
            public void onPlayFinished(String streamId) {
                super.onPlayFinished(streamId);
                broadcastingView.setVisibility(View.GONE);
                startStreamingButton.setText("Start");
                decrementIdle();
            }
        };
    }

    public void sendTextMessage(String messageToSend) {
        final ByteBuffer buffer = ByteBuffer.wrap(messageToSend.getBytes(StandardCharsets.UTF_8));
        DataChannel.Buffer buf = new DataChannel.Buffer(buffer, false);
        webRTCClient.sendMessageViaDataChannel(streamId, buf);
    }

    public void showSendDataChannelMessageDialog(View view) {
        if (webRTCClient != null && webRTCClient.isDataChannelEnabled()) {
            // create an alert builder
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Send Message via Data Channel");
            // set the custom layout
            final View customLayout = getLayoutInflater().inflate(R.layout.send_message_data_channel, null);
            builder.setView(customLayout);
            // add a button
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // send data from the AlertDialog to the Activity
                    EditText editText = customLayout.findViewById(R.id.message_text_input);
                    sendTextMessage(editText.getText().toString());
                }
            });
            // create and show the alert dialog
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        else {
            Toast.makeText(this, R.string.data_channel_not_available, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(webRTCClient != null){
            webRTCClient.stopReconnector();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == PermissionHandler.CAMERA_PERMISSION_REQUEST_CODE){
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                createWebRTCClient();
            } else {
                Toast.makeText(this,"Camera permissions are not granted. Cannot initialize.", Toast.LENGTH_LONG).show();
            }


        }else if(requestCode == PermissionHandler.PUBLISH_PERMISSION_REQUEST_CODE){

            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                startStopStream();
            } else {
                Toast.makeText(this,"Publish permissions are not granted.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
