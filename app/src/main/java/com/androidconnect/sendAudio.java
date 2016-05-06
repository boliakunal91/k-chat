package com.androidconnect;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.androidconnect.sendAudioService;
import com.androidconnect.sendPhotoService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public class sendAudio extends AppCompatActivity {

    private static final String TAG = "SEND AUDIO ACTIVITY";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private TextView mTitle;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private sendAudioService mChatService = null;

    //Variables fro Record Audio
    private static final String AUDIO_RECORDER_FILE_EXT_3GP = ".3gp";
    private static final String AUDIO_RECORDER_FILE_EXT_MP4 = ".mp4";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private MediaRecorder recorder = null;
    private int currentFormat = 0;
    private int output_formats[] = { MediaRecorder.OutputFormat.MPEG_4, MediaRecorder.OutputFormat.THREE_GPP };
    private String file_exts[] = { AUDIO_RECORDER_FILE_EXT_MP4, AUDIO_RECORDER_FILE_EXT_3GP };

    private Button startRecBtn;
    private Button stopRecBtn;
    private Button FormatMp4Btn;
    private String audioFilePath = null;
    private VideoView mVideoView;
    private Button sendAudioButton;
    private static String mFilePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_audio);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Toast.makeText(this, "Bluetooth Adapter Available", Toast.LENGTH_LONG).show();

        if(mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is Not Available", Toast.LENGTH_LONG).show();
            this.finish();
        }
        mTitle = (TextView) findViewById(R.id.mTitle);
        mTitle.setText("Device not Connected! Connect from settings menu.");
        mConversationView = (ListView) findViewById(R.id.in);
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mSendButton = (Button) findViewById(R.id.button_send);

        startRecBtn = (Button) findViewById(R.id.btnStart);

        startRecBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(sendAudio.this, "Recording Started", Toast.LENGTH_LONG).show();
                startRecording();

            }
        });

        stopRecBtn = (Button) findViewById(R.id.btnStop);
        stopRecBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(sendAudio.this, "Recording Stopped", Toast.LENGTH_LONG).show();
                stopRecording();
                mVideoView = (VideoView) findViewById(R.id.video_view);
                mVideoView.setMediaController(new MediaController(sendAudio.this));
                mVideoView.setVideoPath(audioFilePath);
                mVideoView.requestFocus();
                mVideoView.start();

            }
        });

        Button playAudioBtn = (Button) findViewById(R.id.play_audio_btn);
        playAudioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(audioFilePath == null) {
                    Toast.makeText(sendAudio.this, "Please Record Audio First", Toast.LENGTH_SHORT).show();
                    return;
                }
                mVideoView = (VideoView) findViewById(R.id.video_view);
                mVideoView.setMediaController(new MediaController(sendAudio.this));
                mVideoView.setVideoPath(audioFilePath);
                mVideoView.requestFocus();
                mVideoView.start();


            }
        });

        sendAudioButton = (Button) findViewById(R.id.button_send_audio);

        /*
        FormatMp4Btn = (Button) findViewById(R.id.btnFormat);
        FormatMp4Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayFormatDialog();
            }
        });
        */


    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(output_formats[currentFormat]);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        audioFilePath = getFilename();
        recorder.setOutputFile(audioFilePath);
        recorder.setOnErrorListener(errorListener);
        recorder.setOnInfoListener(infoListener);
        try {
            recorder.prepare();
            recorder.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String getFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath() + "/testaudio/";
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);
        if(!file.exists()) {
            file.mkdirs();
        }
        return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + file_exts[currentFormat]);
    }

    private MediaRecorder.OnErrorListener errorListener = new MediaRecorder.OnErrorListener(){

        @Override
        public void onError(MediaRecorder mr, int what, int extra) {
            Toast.makeText(sendAudio.this, "Error" + what + "," + extra, Toast.LENGTH_LONG).show();
        }
    };

    private MediaRecorder.OnInfoListener infoListener = new MediaRecorder.OnInfoListener() {

        @Override
        public void onInfo(MediaRecorder mr, int what, int extra) {
            Toast.makeText(sendAudio.this, "Warning" + what + "," + extra, Toast.LENGTH_LONG).show();
        }
    };

    private void stopRecording() {
        if(null != recorder) {
            recorder.stop();
            recorder.reset();
            recorder.release();
            recorder = null;
        }
    }

    private void displayFormatDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String formats[] = { "MPEG 4", "3GPP" };
        builder.setTitle(getString(R.string.choose_format_title)).setSingleChoiceItems(formats, currentFormat, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                currentFormat = which;
                //setFormatButtonCaption();
                dialog.dismiss();
            }
        }).show();
    }

    //private void setFormatButtonCaption() {
    //    ((Button) findViewById(R.id.btnFormat)).setText(getString(R.string.audio_format) + " (" + file_exts[currentFormat] + ")");
    //}

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if(!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            Toast.makeText(this, "Bluetooth Is Now Enabled", Toast.LENGTH_LONG).show();

        } else if(mChatService == null) {
            //Toast.makeText(this, "You can setup Chat now easily", Toast.LENGTH_LONG).show();
            setupChat();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == sendAudioService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "MYTAG_SetupChat()");

        //Array Adapter for Conversation Thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView.setAdapter(mConversationArrayAdapter);

        //Initialize Edit Message with a listener for return key
        mOutEditText.setOnEditorActionListener(mWriteListener);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView textView = (TextView) findViewById(R.id.edit_text_out);
                String message = textView.getText().toString();
                Log.d(TAG, "MYTAG_Until send message ONCLICK");
                sendMessage(message);
            }
        });

        sendAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView textView = (TextView) findViewById(R.id.edit_text_out);
                String message = textView.getText().toString();
                Log.d(TAG, "MYTAG_Until send Photo ONCLICK");
                sendAudioFile();
            }
        });

        // Initialize the sendAudioService to perform bluetooth connections
        mChatService = new sendAudioService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    private void sendAudioFile() {

        Log.d(TAG, "MYTAG_Entered send Audio works fine");
        if (mChatService.getState() != sendPhotoService.STATE_CONNECTED) {
            Toast.makeText(this, "You are not Connected",Toast.LENGTH_SHORT).show();
            return;
        }
        if (audioFilePath == null)
        {
            Toast.makeText(this, "Please Record Audio First", Toast.LENGTH_LONG).show();
            return;
        }

        TextView textView = (TextView) findViewById(R.id.edit_text_out);
        textView.setText("Audio Sent!");
        mFilePath = audioFilePath;
        File f = new File(mFilePath);
        Log.d(TAG, "MYTAG_File path fetched and photo created" + mFilePath);
        byte[] byteArray = null;
        if(f.exists()) {
            try {
                InputStream inputStream = new FileInputStream(f);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] b = new byte[1024*8];
                int bytesRead = 0;

                while ((bytesRead = inputStream.read(b)) != -1) {
                    bos.write(b, 0, bytesRead);
                }

                byteArray = bos.toByteArray();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            byte[] audio = byteArray;
            Toast.makeText(this,"Audio Sent!", Toast.LENGTH_SHORT).show();
            mChatService.write(audio);

        }
    }


    private void sendMessage(String message) {

        Log.d(TAG, "MYTAG_Entered send message works fine");
        if (mChatService.getState() != sendAudioService.STATE_CONNECTED) {
            Toast.makeText(this, "You are not Connected",Toast.LENGTH_LONG).show();
            return;
        }

        if(message.length() > 0) {
            byte[] send = message.getBytes();
            mChatService.write(send);

            //RESET out string buffer to zero and clear edit text
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                Log.d(TAG, "MYTAG_Until send message OnEnter");
                sendMessage(message);
            }
            return true;

        }


    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {

        final ActionBar actionBar = this.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = this.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the sendAudioService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case sendAudioService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case sendAudioService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case sendAudioService.STATE_LISTEN:
                        case sendAudioService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    Toast.makeText(sendAudio.this, "Audio Received Unable to playback", Toast.LENGTH_SHORT).show();
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    //String readMessage = new String(readBuf, 0, msg.arg1);
                    //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    try{
                        String filename= System.currentTimeMillis()+".mp4";
                        String root = Environment.getExternalStorageState().toString();
                        File myDir = new File(root+"/AndroidConnect");
                        if(!myDir.exists())
                            myDir.mkdir();
                        File file = new File(myDir, filename);

                        FileOutputStream out = new FileOutputStream(file);
                        out.write(readBuf);
                        out.close();

                        mVideoView = (VideoView) findViewById(R.id.video_view);
                        mVideoView.setMediaController(new MediaController(sendAudio.this));
                        Uri video = Uri.parse(file.getPath());
                        mVideoView.setVideoURI(video);
                        mVideoView.requestFocus();
                        mVideoView.start();


                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != getParent()) {
                        Toast.makeText(getParent(), "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != getParent()) {
                        Toast.makeText(getParent(), msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resutCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                if(resutCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;

            case REQUEST_ENABLE_BT:
                if(resutCode == Activity.RESULT_OK) {
                    setupChat();
                } else {
                    Log.d(TAG, "MYTAG_BT NOT ENABLEd");
                    Toast.makeText(this, "Bluetooth was not enabled. Closing application", Toast.LENGTH_LONG).show();
                    finish();

                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bluetooth_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                mTitle.setText("Device Connected");
                return true;
            }

            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }
    private void ensureDiscoverable() {
        if(mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 250);
            startActivity(discoverableIntent);
            Log.d(TAG, "MYTAG_MakeDiscoverable Succesfull");
            Toast.makeText(this, "Make Discoverable Menu Works Fine.", Toast.LENGTH_LONG).show();
        }
    }


}
