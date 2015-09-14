package ai.qed.gidfinder;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private TextView mLocationView;

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0 * 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 0 * 1000 * 60 * 1; // 1 minute

    @Override
    protected void onCreate(Bundle savedInstanceState) throws SecurityException {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLocationView = (TextView)findViewById(R.id.coordinates);

        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                GridID gridID = GridID.fromLocation(location);
                MainActivity.this.setLocationMessage(gridID.toString());
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

        mLocationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
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
        } else {
            // TODO
        }
    }

    private void stopTracking() {
        mLocationManager.removeUpdates(mLocationListener);
    }

    private void setLocationMessage(final String message) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLocationView.setText(message);
            }
        });
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

