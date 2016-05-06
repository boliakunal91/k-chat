package com.androidconnect;

/**
 * Created by GagandeepSingh on 9/26/2015.
 */
public interface Constants {
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";




    //Constants for Send Photo (keys)
    public static int write_size = 512;
    public static int read_size = 1024;
    public static long load_speeds = 200;

    public static final String start_md5 = "<md5>";
    public static final String receive_md5 = "<receive_md5>";
    public static final String send_content = "<send_content>";
    public static final String end_content = "<end_content>";
    public static final String send_end = "<send_end>";
}
