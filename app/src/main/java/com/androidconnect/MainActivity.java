package com.androidconnect;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.androidconnect.sendAudio;
import com.androidconnect.sendPhoto;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void textMessage(View view){
        Intent textIntent = new Intent(this, BluetoothChat.class);
        startActivity(textIntent);
    }

    public void sendPhoto(View view) {
        Intent sendPhotoIntent = new Intent(this, sendPhoto.class);
        startActivity(sendPhotoIntent);
    }

    public void sendAudio(View view) {
        Intent sendAudioIntent = new Intent(this, sendAudio.class);
        startActivity(sendAudioIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
