package com.example.rohit.arduinofinal1;

import android.bluetooth.BluetoothAdapter;

import android.content.Context;
import android.content.Intent;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.Sensor;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.view.View;
import android.widget.Button;
import android.hardware.SensorEventListener;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.lang.Math;



public class MainActivity extends AppCompatActivity implements SensorEventListener {

    SensorManager sManager;
    BluetoothAdapter bleAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice dvce;
    OutputStream outptStrm;
    InputStream inptStrm;

    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;

    private static final float max_gravity = 2.8F;
    private static final int slope_time = 600;
    private long ShktTime;
    String msg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            sManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);

            Sensor proximty;
            proximty = sManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

            Sensor acclerometer;
            acclerometer = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            sManager.registerListener(this, proximty, SensorManager.SENSOR_DELAY_NORMAL);
            sManager.registerListener(this, acclerometer, SensorManager.SENSOR_DELAY_UI);

            Button press = (Button) findViewById(R.id.connectBluetooth);
            press.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    connectBluetooth();
                }
            });
        } catch (Exception ex) {
            String s = "error";
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            long mLastShakeTime = 0;

            //if sensor is proximity
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                Toast.makeText(getApplicationContext(), "Proximity", Toast.LENGTH_SHORT).show();
                msg = "1\n";
                outptStrm.write(msg.getBytes());

            } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                //if sensor is accelerometer
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                //calculate gravity parameters
                float gravityX = x / SensorManager.GRAVITY_EARTH;
                float gravityY = y / SensorManager.GRAVITY_EARTH;
                float gravityZ = z / SensorManager.GRAVITY_EARTH;

                float gForce = (float) Math.sqrt(gravityX * gravityX + gravityY * gravityY + gravityZ * gravityZ);

                if (gForce > max_gravity) {
                    final long now = System.currentTimeMillis();

                    //ignore shakes very near to each other
                    if (ShktTime + slope_time > now) {
                        return;
                    }
                    ShktTime = now;

                    Toast.makeText(getApplicationContext(), "Shake", Toast.LENGTH_SHORT).show();
                    msg = "2\n";
                    outptStrm.write(msg.getBytes());
                }

            }

            //outptStrm.write(msg.getBytes());
            //String msg = "2";
            //msg += "\n";
            //outptStrm.write(msg.getBytes());
        } catch (Exception ex) {
            String s = "";
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void showToast(String sensor) {
        try {
            Context context = getApplicationContext();
            CharSequence text = sensor;
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        } catch (Exception ex) {

        }
    }

    public void connectBluetooth() {
        try {
            bleAdapter = BluetoothAdapter.getDefaultAdapter();

            if (!bleAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, 0);
            }

            Set<BluetoothDevice> pairedDevices = bleAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().equals("HC-05")) {
                        dvce = device;
                        break;
                    }
                }
            }

            bleAdapter.cancelDiscovery();

            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            mmSocket = dvce.createInsecureRfcommSocketToServiceRecord(uuid);

            bleAdapter.cancelDiscovery();
            mmSocket.connect();

            outptStrm = mmSocket.getOutputStream();
            inptStrm = mmSocket.getInputStream();

            beginListenForData();

            Toast.makeText(getApplicationContext(), "Arduino Connected", Toast.LENGTH_SHORT).show();

        } catch (Exception ex) {

        }
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = inptStrm.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            inptStrm.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            //myLabel.setText(data);
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }
}
