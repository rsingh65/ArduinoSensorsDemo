package com.example.rohit.arduinofinal1;

import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.Sensor;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    SensorManager sManager;
    TextView xValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            sManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);

            Sensor proximty;

            proximty = sManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

            sManager.registerListener(this, proximty, SensorManager.SENSOR_DELAY_NORMAL);
        }
        catch (Exception ex) {
            String s = "";
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        try {
            xValue = findViewById(R.id.xTextView);

            xValue.setText(Float.toString(event.values[0]));

        }
        catch(Exception ex) {
            String s = "";
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
