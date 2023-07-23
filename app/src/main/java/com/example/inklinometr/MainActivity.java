package com.example.inklinometr;


import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private boolean isRotationVectorSensorAvailable = false;
    private final float[] rotationMatrix = new float[9];
    private final float[] initialInclinationValues = new float[2];
    private final float[] currentInclinationValues = new float[2];
    private boolean isRelativeAngleMode = false;
    private TextView inclinationTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Force landscape orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        inclinationTextView = findViewById(R.id.inclinationTextView);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        // Check if the rotation vector sensor is available on the device
        if (rotationVectorSensor != null) {
            isRotationVectorSensorAvailable = true;
        } else {
            isRotationVectorSensorAvailable = false;
            inclinationTextView.setText(R.string.noSensors);
            inclinationTextView.setTextColor(Color.RED); // Show error message in red color
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isRotationVectorSensorAvailable) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRotationVectorSensorAvailable) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == rotationVectorSensor) {
            // Get the rotation matrix from the rotation vector sensor data
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

            // Remap the coordinate system to handle the y-axis inclination (screen surface)
            float[] remappedMatrix = new float[9];
            SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedMatrix);

            // Calculate the inclination angle using the remapped rotation matrix
            currentInclinationValues[0] = (float) Math.atan2(remappedMatrix[1], remappedMatrix[4]);
            currentInclinationValues[1] = (float) Math.atan2(-remappedMatrix[6], remappedMatrix[8]);

            if (isRelativeAngleMode) {
                // Calculate the relative inclination angle in degrees
                //float relativeInclinationX = (float) Math.toDegrees(currentInclinationValues[0] - initialInclinationValues[0]);
                float relativeInclinationY = (float) Math.toDegrees(currentInclinationValues[1] - initialInclinationValues[1]);
                relativeInclinationY = Math.round(relativeInclinationY * 10.0) / 10.0f;


                // Update the inclinationTextView with the calculated relative angle
                inclinationTextView.setText(String.format("%s°", relativeInclinationY));

            } else {
                // Calculate the inclination angle in degrees
                float inclinationAngle = (float) Math.toDegrees(Math.acos(remappedMatrix[8]));
                inclinationAngle = Math.round(inclinationAngle * 10 - 900) / 10.0f;

                // Update the inclinationTextView with the calculated angle
                inclinationTextView.setText(String.format("%s°", inclinationAngle));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle changes in sensor accuracy if needed.
    }

    public void setZeroAngle(View view) {
        // Set the current inclination as the relative zero
        System.arraycopy(currentInclinationValues, 0, initialInclinationValues, 0, currentInclinationValues.length);
        isRelativeAngleMode = true;
    }

    public void resetToDefault(View view) {
        // Reset to default mode (show absolute angles)
        isRelativeAngleMode = false;
    }
}
