package ai.qed.gidfinder;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private TextView mLocationView;
    private TextView cell1;
    private TextView cell2;
    private TextView cell3;
    private TextView cell4;
    private TextView cell5;
    private TextView cell6;
    private TextView cell7;
    private TextView cell8;
    private TextView cell9;
    private SensorManager mSensorManager;
    private SensorEventListener mSensorEventListener;
    private ImageView marker;
    private RelativeLayout parentView;
    private int perUnitPixel;

    // record the compass picture angle turned
    private float currentDegree = 0f;

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0 * 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 0 * 1000 * 60 * 1; // 1 minute

    @Override
    protected void onCreate(Bundle savedInstanceState) throws SecurityException {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mLocationView = (TextView) findViewById(R.id.coordinates);
        cell1 = (TextView) findViewById(R.id.cell_1);
        cell2 = (TextView) findViewById(R.id.cell_2);
        cell3 = (TextView) findViewById(R.id.cell_3);
        cell4 = (TextView) findViewById(R.id.cell_4);
        cell5 = (TextView) findViewById(R.id.cell_5);
        cell6 = (TextView) findViewById(R.id.cell_6);
        cell7 = (TextView) findViewById(R.id.cell_7);
        cell8 = (TextView) findViewById(R.id.cell_8);
        cell9 = (TextView) findViewById(R.id.cell_9);
        marker = (ImageView) findViewById(R.id.marker);
        parentView = (RelativeLayout) findViewById(R.id.parent_view);
        perUnitPixel = (int) getResources().getDimension(R.dimen.per_unit_dp);

        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                GridID gridID = GridID.fromLocation(location);
                MainActivity.this.setLocationMessage(gridID.toString(), gridID.getSubcell());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        mSensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                // get the angle around the z-axis rotated
                float degree = Math.round(event.values[0]);

                // create a rotation animation (reverse turn degree degrees)
                RotateAnimation ra = new RotateAnimation(
                        currentDegree,
                        -degree,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.75f);

                // how long the animation will take place
                ra.setDuration(100);

                // set the animation after the end of the reservation status
                ra.setFillAfter(true);

                // Start the animation
                marker.startAnimation(ra);
                currentDegree = -degree;

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        mLocationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    private void startTracking() {
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    mLocationListener);
            Location latestLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(latestLocation != null) {
                mLocationListener.onLocationChanged(latestLocation);
            }

            // for the system's orientation sensor registered listeners
            mSensorManager.registerListener(mSensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                    SensorManager.SENSOR_DELAY_GAME);
        } else {
            // TODO
        }
    }

    private void stopTracking() {
        mLocationManager.removeUpdates(mLocationListener);
        mSensorManager.unregisterListener(mSensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION));
    }

    private void setLocationMessage(final String message, final int subcell) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLocationView.setText(message);

                setCells(subcell);
                positionMarker(subcell);
            }
        });
    }

    private void positionMarker(int subcell) {
        marker.setVisibility(View.VISIBLE);
        parentView.removeView(marker);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) marker.getLayoutParams();
        params.setMargins((subcell / 10) * perUnitPixel, (9 - (subcell % 10)) * perUnitPixel - (int) (params.height * 0.65), 0, 0);
        parentView.addView(marker, params);
    }

    private void setCells(int subcell) {
        // corner cells
        if (subcell == 0) {
            setCell(cell1, Integer.toString(subcell + 2), false);
            setCell(cell2, Integer.toString(subcell + 12), false);
            setCell(cell3, Integer.toString(subcell + 22), false);
            setCell(cell4, Integer.toString(subcell + 1), false);
            setCell(cell5, Integer.toString(subcell + 11), false);
            setCell(cell6, Integer.toString(subcell + 21), false);
            setCell(cell7, Integer.toString(subcell), true);
            setCell(cell8, Integer.toString(subcell + 10), false);
            setCell(cell9, Integer.toString(subcell + 20), false);
        }
        else if (subcell == 9) {
            setCell(cell1, Integer.toString(subcell), true);
            setCell(cell2, Integer.toString(subcell + 10), false);
            setCell(cell3, Integer.toString(subcell + 20), false);
            setCell(cell4, Integer.toString(subcell - 1), false);
            setCell(cell5, Integer.toString(subcell + 9), false);
            setCell(cell6, Integer.toString(subcell + 19), false);
            setCell(cell7, Integer.toString(subcell - 2), false);
            setCell(cell8, Integer.toString(subcell + 8), false);
            setCell(cell9, Integer.toString(subcell + 18), false);
        }
        else if (subcell == 99) {
            setCell(cell1, Integer.toString(subcell - 20), false);
            setCell(cell2, Integer.toString(subcell - 10), false);
            setCell(cell3, Integer.toString(subcell), true);
            setCell(cell4, Integer.toString(subcell - 21), false);
            setCell(cell5, Integer.toString(subcell - 11), false);
            setCell(cell6, Integer.toString(subcell - 1), false);
            setCell(cell7, Integer.toString(subcell - 22), false);
            setCell(cell8, Integer.toString(subcell - 12), false);
            setCell(cell9, Integer.toString(subcell - 2), false);
        }
        else if (subcell == 90) {
            setCell(cell1, Integer.toString(subcell - 18), false);
            setCell(cell2, Integer.toString(subcell - 8), false);
            setCell(cell3, Integer.toString(subcell + 2), false);
            setCell(cell4, Integer.toString(subcell - 19), false);
            setCell(cell5, Integer.toString(subcell - 9), false);
            setCell(cell6, Integer.toString(subcell + 1), false);
            setCell(cell7, Integer.toString(subcell - 20), false);
            setCell(cell8, Integer.toString(subcell - 10), false);
            setCell(cell9, Integer.toString(subcell), true);
        }
        // left side
        else if (subcell < 10) {
            setCell(cell1, Integer.toString(subcell + 1), false);
            setCell(cell2, Integer.toString(subcell + 11), false);
            setCell(cell3, Integer.toString(subcell + 21), false);
            setCell(cell4, Integer.toString(subcell), true);
            setCell(cell5, Integer.toString(subcell + 10), false);
            setCell(cell6, Integer.toString(subcell + 20), false);
            setCell(cell7, Integer.toString(subcell - 1), false);
            setCell(cell8, Integer.toString(subcell + 9), false);
            setCell(cell9, Integer.toString(subcell + 19), false);
        }
        // top side
        else if ((subcell % 10) == 9) {
            setCell(cell1, Integer.toString(subcell - 10), false);
            setCell(cell2, Integer.toString(subcell), true);
            setCell(cell3, Integer.toString(subcell + 10), false);
            setCell(cell4, Integer.toString(subcell - 11), false);
            setCell(cell5, Integer.toString(subcell - 1), false);
            setCell(cell6, Integer.toString(subcell + 9), false);
            setCell(cell7, Integer.toString(subcell - 12), false);
            setCell(cell8, Integer.toString(subcell - 2), false);
            setCell(cell9, Integer.toString(subcell + 8), false);
        }
        // right side
        else if (subcell > 90) {
            setCell(cell1, Integer.toString(subcell - 19), false);
            setCell(cell2, Integer.toString(subcell - 9), false);
            setCell(cell3, Integer.toString(subcell + 1), false);
            setCell(cell4, Integer.toString(subcell - 20), false);
            setCell(cell5, Integer.toString(subcell - 10), false);
            setCell(cell6, Integer.toString(subcell), true);
            setCell(cell7, Integer.toString(subcell - 21), false);
            setCell(cell8, Integer.toString(subcell - 11), false);
            setCell(cell9, Integer.toString(subcell - 1), false);
        }
        // bottom side
        else if ((subcell % 10) == 0) {
            setCell(cell1, Integer.toString(subcell - 8), false);
            setCell(cell2, Integer.toString(subcell + 2), false);
            setCell(cell3, Integer.toString(subcell + 12), false);
            setCell(cell4, Integer.toString(subcell - 9), false);
            setCell(cell5, Integer.toString(subcell + 1), false);
            setCell(cell6, Integer.toString(subcell + 11), false);
            setCell(cell7, Integer.toString(subcell - 10), false);
            setCell(cell8, Integer.toString(subcell), true);
            setCell(cell9, Integer.toString(subcell + 10), false);
        }
        // middle cells
        else {
            setCell(cell1, Integer.toString(subcell - 9), false);
            setCell(cell2, Integer.toString(subcell + 1), false);
            setCell(cell3, Integer.toString(subcell + 11), false);
            setCell(cell4, Integer.toString(subcell - 10), false);
            setCell(cell5, Integer.toString(subcell), true);
            setCell(cell6, Integer.toString(subcell + 10), false);
            setCell(cell7, Integer.toString(subcell - 11), false);
            setCell(cell8, Integer.toString(subcell - 1), false);
            setCell(cell9, Integer.toString(subcell + 9), false);
        }
    }

    private void setCell(TextView cell, String value, boolean isCurrent) {
        cell.setText(value);
        if (isCurrent) {
            cell.setBackgroundResource(R.drawable.center_bg);
        }
        else {
            cell.setBackgroundDrawable(null);
        }
    }

    @Override
    protected void onStart() {
        startTracking();
        super.onStart();
    }

    @Override
    protected void onPause() {
        stopTracking();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopTracking();
        super.onDestroy();
    }
}

