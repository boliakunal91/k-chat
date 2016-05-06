package com.androidconnect;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidconnect.sendPhotoService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class sendPhoto extends AppCompatActivity {

    private static final String TAG = "SEND PHOTO ACTIVITY";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private TextView mTitle;
    private ImageView image;
    private Button sendPhotoButton;
    private Button selectPhotoButton;
    private String selectedPhotoPath = null;
    private static final int SELECT_PICTURE_CODE = 10;
    Display display;

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
    private sendPhotoService mChatService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_photo);
        Context context = getApplicationContext();
        display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
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
        image = (ImageView) findViewById(R.id.image_view);
        sendPhotoButton = (Button) findViewById(R.id.button_send_photo);


        //Select Photo Button Code
        selectPhotoButton = (Button) findViewById(R.id.selectPhotoButton);
        selectPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent selectPhotoIntent = new Intent();
                selectPhotoIntent.setType("image/*");
                selectPhotoIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(selectPhotoIntent,"Select Picture"),SELECT_PICTURE_CODE );
            }
        });


    }

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
            if (mChatService.getState() == sendPhotoService.STATE_NONE) {
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

        sendPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView textView = (TextView) findViewById(R.id.edit_text_out);
                String message = textView.getText().toString();
                Log.d(TAG, "MYTAG_Until send Photo ONCLICK");
                textView.setText("Picture Sent!");
                sendPicture();
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new sendPhotoService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }



    private void sendMessage(String message) {

        Log.d(TAG, "MYTAG_Entered send message works fine");
        if (mChatService.getState() != sendPhotoService.STATE_CONNECTED) {
            Toast.makeText(this, "You are not Connected",Toast.LENGTH_SHORT).show();
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

    static String mFilePath;

    private void sendPicture() {

        Log.d(TAG, "MYTAG_Entered send Picture works fine");
        if (mChatService.getState() != sendPhotoService.STATE_CONNECTED) {
            Toast.makeText(this, "You are not Connected. Try Again",Toast.LENGTH_LONG).show();
            return;
        }
        if (selectedPhotoPath == null)
        {
            Toast.makeText(this, "Please Select Picture First", Toast.LENGTH_LONG).show();
            return;
        }

        mFilePath = selectedPhotoPath;
        File f = new File(mFilePath);
        Log.d(TAG, "MYTAG_File path fetched and photo created" + mFilePath);
        if(f.exists()) {
            Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 40, bytes);
            byte[] image = bytes.toByteArray();
            mChatService.write(image);

        }
    }




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
     * The Handler that gets information back from the BluetoothChatService
     */


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case sendPhotoService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case sendPhotoService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case sendPhotoService.STATE_LISTEN:
                        case sendPhotoService.STATE_NONE:
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
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    //String readMessage = new String(readBuf, 0, msg.arg1);
                    //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    Bitmap bmp = BitmapFactory.decodeByteArray(readBuf, 0 , msg.arg1);
                    image.setImageBitmap(bmp);
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                if(resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;

            case REQUEST_ENABLE_BT:
                if(resultCode == Activity.RESULT_OK) {
                    setupChat();
                } else {
                    Log.d(TAG, "MYTAG_BT NOT ENABLEd");
                    Toast.makeText(this, "Bluetooth was not enabled. Closing application", Toast.LENGTH_LONG).show();
                    finish();

                }
                break;

            case SELECT_PICTURE_CODE:
                if(resultCode == RESULT_OK)
                {
                    Uri selectedImageUri = data.getData();
                    selectedPhotoPath = getPath(selectedImageUri);
                    System.out.println("Image Path: " + selectedPhotoPath);
                    image.setImageURI(selectedImageUri);
                }
        }
    }

    public String getPath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
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

    public Bitmap resizeBitmap(Bitmap bitmap, int newWidth, int newHeight){



        int width = bitmap.getWidth();
        int height = bitmap.getHeight();


        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;


        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);



        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);




        return  bitmap;

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
