package com.example.anne.testvisit;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.content.Intent;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    Button btn;
    Speaker speaker;
    static public BluetoothDevice mmDevice;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BTConnect(mBluetoothAdapter);

        speaker = new Speaker(this);

        //Begin Button
        btn = (Button) findViewById(R.id.button);

        btn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                goToPlanYourTrip();
                return true;
            }
        });

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                speaker.allow(true);
                speaker.speak("Welcome to I visit. Long click anywhere on the screen to start. ");
            }
        });

    }

    // Bluetooth Device Connecting
    public void BTConnect(BluetoothAdapter mBluetoothAdapter) {
        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("raspberrypi")) //Note, you will need to change this to match the name of your device
                {
                    Log.e("IVisit",device.getName());
                    Toast.makeText(MainActivity.this, "Connected with RaspberryPi", Toast.LENGTH_SHORT).show();
                    mmDevice = device;
                    break;
                }
            }
        }
    }

    private void goToPlanYourTrip() {
        Intent intent = new Intent(this, PlanYourTrip.class);
        startActivity(intent);

    }


    protected void onStop() {
        super.onStop();
        speaker.destroy();
    }

}


