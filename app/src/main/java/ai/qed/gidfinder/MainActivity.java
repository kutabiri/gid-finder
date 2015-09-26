package ai.qed.gidfinder;

import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private TextView gidTextView;
    private TextView latTextView;
    private TextView longTextView;
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
    private View marker;
    private RelativeLayout parentView;
    private GridID currentGID = null;
    private MediaPlayer pingSound;
    private ImageView compass;
    private SharedPreferences preferences;
    private Button zoomInButton;
    private Button zoomOutButton;
    private CheckBox pingToggle;
    private GridView mapGrid;
    private View singleGrid;
    private int zoomLevel = 1;
    private Handler handler;

    // record the compass picture angle turned
    private float currentDegree = 0f;

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0 * 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 0 * 1000 * 60 * 1; // 1 minute

    private static final List<String> largeGridArray = new ArrayList<>();

    static {
        for (int i = 0; i < 10; i ++) {
            for (int j = 0; j < 10; j++) {
                largeGridArray.add(i * 10 + j, Integer.toString(j * 10 + 9 - i));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) throws SecurityException {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.status_bar_color));
        }

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        pingSound = MediaPlayer.create(this, R.raw.ping);
        handler = new Handler(getMainLooper());
        gidTextView = (TextView) findViewById(R.id.coordinates);
        latTextView = (TextView) findViewById(R.id.latitude);
        longTextView = (TextView) findViewById(R.id.longitude);
        cell1 = (TextView) findViewById(R.id.cell_1);
        cell2 = (TextView) findViewById(R.id.cell_2);
        cell3 = (TextView) findViewById(R.id.cell_3);
        cell4 = (TextView) findViewById(R.id.cell_4);
        cell5 = (TextView) findViewById(R.id.cell_5);
        cell6 = (TextView) findViewById(R.id.cell_6);
        cell7 = (TextView) findViewById(R.id.cell_7);
        cell8 = (TextView) findViewById(R.id.cell_8);
        cell9 = (TextView) findViewById(R.id.cell_9);
        marker = getLayoutInflater().inflate(R.layout.marker, null);
        parentView = (RelativeLayout) findViewById(R.id.parent_view);
        compass = (ImageView) findViewById(R.id.compass);
        pingToggle = (CheckBox) findViewById(R.id.ping_toggle);
        mapGrid = (GridView) findViewById(R.id.map_section);
        singleGrid = findViewById(R.id.small_grid);

        boolean pingOn = preferences.getBoolean(getString(R.string.pref_key_sound), true);

        if (pingOn) {
            pingToggle.setChecked(true);
            pingToggle.setText(R.string.ping_on);
        }
        else {
            pingToggle.setChecked(false);
            pingToggle.setText(R.string.ping_off);
        }

        pingToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    pingToggle.setText(R.string.ping_on);
                }
                else {
                    pingToggle.setText(R.string.ping_off);
                }
                preferences.edit().putBoolean(getString(R.string.pref_key_sound), isChecked).apply();
            }
        });

        zoomInButton = (Button) findViewById(R.id.zoom_in_btn);
        zoomOutButton = (Button) findViewById(R.id.zoom_out_btn);

        zoomInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentGID != null) {
                    zoomLevel--;

                    updateZoomButtons();
                }
            }
        });

        zoomOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentGID != null) {
                    zoomLevel++;

                    updateZoomButtons();
                }
            }
        });

        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                GridID gridID = GridID.fromLocation(location);
                MainActivity.this.setLocationMessage(gridID);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {
                gidTextView.setText(getString(R.string.initial_message));
            }

            @Override
            public void onProviderDisabled(String provider) {
                handler.removeCallbacksAndMessages(null);
                gidTextView.setText(getString(R.string.please_turn_on_gps));
                currentGID = null;
                latTextView.setText(null);
                longTextView.setText(null);
                parentView.removeView(marker);
                cell1.setText(null);
                cell2.setText(null);
                cell3.setText(null);
                cell4.setText(null);
                cell5.setText(null);
                cell6.setText(null);
                cell7.setText(null);
                cell8.setText(null);
                cell9.setText(null);
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
                        Animation.RELATIVE_TO_SELF, 0.5f);

                // how long the animation will take place
                ra.setDuration(100);

                // set the animation after the end of the reservation status
                ra.setFillAfter(true);

                // Start the animation
                compass.startAnimation(ra);
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

    private void updateZoomButtons() {
        parentView.removeView(marker);
        if (zoomLevel == 1) {
            zoomInButton.setEnabled(false);
            mapGrid.setVisibility(View.GONE);
            singleGrid.setVisibility(View.VISIBLE);
            ((TextView) singleGrid.findViewById(R.id.cell_number)).setText(Integer.toString(currentGID.getSubcell()));
        }
        else {
            List<String> list;
            mapGrid.setVisibility(View.VISIBLE);
            singleGrid.setVisibility(View.GONE);

            if (zoomLevel == 3) {
                zoomOutButton.setEnabled(false);
                mapGrid.setNumColumns(10);
                list = largeGridArray;
            } else {
                list = getCellList(currentGID.getSubcell());
                zoomInButton.setEnabled(true);
                zoomOutButton.setEnabled(true);
                mapGrid.setNumColumns(3);
            }

            mapGrid.setAdapter(new GridAdapter<>(this, list, zoomLevel, Integer.toString(currentGID.getSubcell())));
        }

        refreshMarker();
    }

    private void refreshMarker() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                positionMarker(currentGID);
            }
        }, 500);
    }

    private void startTracking() {
        // for the system's orientation sensor registered listeners
        mSensorManager.registerListener(mSensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);

        mLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_BW_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES,
                mLocationListener);
        Location latestLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (latestLocation != null) {
            mLocationListener.onLocationChanged(latestLocation);
        }

        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gidTextView.setText(getString(R.string.please_turn_on_gps));
        }
    }

    private void stopTracking() {
        mLocationManager.removeUpdates(mLocationListener);
        mSensorManager.unregisterListener(mSensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION));
    }

    private void setLocationMessage(final GridID gridId) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // only need to ping and update gid when it changed
                if (gridId != null && (currentGID == null || !gridId.toGIDString().equals(currentGID.toGIDString()))) {

                    if (preferences.getBoolean(getString(R.string.pref_key_sound), true)) {
                        pingSound.start();
                    }

                    gidTextView.setText(gridId.toGIDString());

                    if (zoomLevel == 1) {
                        ((TextView) singleGrid.findViewById(R.id.cell_number)).setText(Integer.toString(gridId.getSubcell()));
                    }
                }

                latTextView.setText(gridId.getLatString());
                longTextView.setText(gridId.getLongString());

                currentGID = gridId;
                refreshMarker();
            }
        });
    }

    private void positionMarker(GridID gridID) {
        parentView.removeView(marker);
        int markerSize = (int) getResources().getDimension(R.dimen.marker_width);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(markerSize, markerSize);

        int leftMargin;
        int topMargin;
        if (zoomLevel == 1) {
            leftMargin = gridID.getXOffset() * singleGrid.getWidth() / 100 - (markerSize / 2) + singleGrid.getLeft();
            topMargin = (100 - gridID.getYOffset()) * singleGrid.getHeight() / 100 - (markerSize / 2) + singleGrid.getTop();
        }
        else {
            View view = null;
            for (int i = 0; i < mapGrid.getCount(); i++) {
                String item = (String) mapGrid.getAdapter().getItem(i);
                if (item.equals(Integer.toString(currentGID.getSubcell()))) {
                    view = mapGrid.getChildAt(i);
                    break;
                }
            }

            if (zoomLevel == 2) {
                leftMargin = gridID.getXOffset() * view.getWidth() / 100 - (markerSize / 2) + view.getLeft() + mapGrid.getLeft();
                topMargin = (100 - gridID.getYOffset()) * view.getHeight() / 100 - (markerSize / 2) + view.getTop() + mapGrid.getTop();
            } else {
                leftMargin = view.getWidth() / 2 - (markerSize / 2) + view.getLeft() + mapGrid.getLeft();
                topMargin = view.getHeight() / 2 - (markerSize / 2) + view.getTop() + mapGrid.getTop();
            }
        }

        params.setMargins(leftMargin, topMargin, 0, 0);
        marker.setLayoutParams(params);
        parentView.addView(marker);
    }

    private List<String> getCellList(int subcell) {
        List<String> cells = new ArrayList<>();

        // corner cells
        if (subcell == 0) {
            cells.add(0, Integer.toString(subcell + 2));
            cells.add(1, Integer.toString(subcell + 12));
            cells.add(2, Integer.toString(subcell + 22));
            cells.add(3, Integer.toString(subcell + 1));
            cells.add(4, Integer.toString(subcell + 11));
            cells.add(5, Integer.toString(subcell + 21));
            cells.add(6, Integer.toString(subcell));
            cells.add(7, Integer.toString(subcell + 10));
            cells.add(8, Integer.toString(subcell + 20));
        }
        else if (subcell == 9) {
            cells.add(0, Integer.toString(subcell));
            cells.add(1, Integer.toString(subcell + 10));
            cells.add(2, Integer.toString(subcell + 20));
            cells.add(3, Integer.toString(subcell - 1));
            cells.add(4, Integer.toString(subcell + 9));
            cells.add(5, Integer.toString(subcell + 19));
            cells.add(6, Integer.toString(subcell - 2));
            cells.add(7, Integer.toString(subcell + 8));
            cells.add(8, Integer.toString(subcell + 18));
        }
        else if (subcell == 99) {
            cells.add(0, Integer.toString(subcell - 20));
            cells.add(1, Integer.toString(subcell - 10));
            cells.add(2, Integer.toString(subcell));
            cells.add(3, Integer.toString(subcell - 21));
            cells.add(4, Integer.toString(subcell - 11));
            cells.add(5, Integer.toString(subcell - 1));
            cells.add(6, Integer.toString(subcell - 22));
            cells.add(7, Integer.toString(subcell - 12));
            cells.add(8, Integer.toString(subcell - 2));
        }
        else if (subcell == 90) {
            cells.add(0, Integer.toString(subcell - 18));
            cells.add(1, Integer.toString(subcell - 8));
            cells.add(2, Integer.toString(subcell + 2));
            cells.add(3, Integer.toString(subcell - 19));
            cells.add(4, Integer.toString(subcell - 9));
            cells.add(5, Integer.toString(subcell + 1));
            cells.add(6, Integer.toString(subcell - 20));
            cells.add(7, Integer.toString(subcell - 10));
            cells.add(8, Integer.toString(subcell));
        }
        // left side
        else if (subcell < 10) {
            cells.add(0, Integer.toString(subcell + 1));
            cells.add(1, Integer.toString(subcell + 11));
            cells.add(2, Integer.toString(subcell + 21));
            cells.add(3, Integer.toString(subcell));
            cells.add(4, Integer.toString(subcell + 10));
            cells.add(5, Integer.toString(subcell + 20));
            cells.add(6, Integer.toString(subcell - 1));
            cells.add(7, Integer.toString(subcell + 9));
            cells.add(8, Integer.toString(subcell + 19));
        }
        // top side
        else if ((subcell % 10) == 9) {
            cells.add(0, Integer.toString(subcell - 10));
            cells.add(1, Integer.toString(subcell));
            cells.add(2, Integer.toString(subcell + 10));
            cells.add(3, Integer.toString(subcell - 11));
            cells.add(4, Integer.toString(subcell - 1));
            cells.add(5, Integer.toString(subcell + 9));
            cells.add(6, Integer.toString(subcell - 12));
            cells.add(7, Integer.toString(subcell - 2));
            cells.add(8, Integer.toString(subcell + 8));
        }
        // right side
        else if (subcell > 90) {
            cells.add(0, Integer.toString(subcell - 19));
            cells.add(1, Integer.toString(subcell - 9));
            cells.add(2, Integer.toString(subcell + 1));
            cells.add(3, Integer.toString(subcell - 20));
            cells.add(4, Integer.toString(subcell - 10));
            cells.add(5, Integer.toString(subcell));
            cells.add(6, Integer.toString(subcell - 21));
            cells.add(7, Integer.toString(subcell - 11));
            cells.add(8, Integer.toString(subcell - 1));
        }
        // bottom side
        else if ((subcell % 10) == 0) {
            cells.add(0, Integer.toString(subcell - 8));
            cells.add(1, Integer.toString(subcell + 2));
            cells.add(2, Integer.toString(subcell + 12));
            cells.add(3, Integer.toString(subcell - 9));
            cells.add(4, Integer.toString(subcell + 1));
            cells.add(5, Integer.toString(subcell + 11));
            cells.add(6, Integer.toString(subcell - 10));
            cells.add(7, Integer.toString(subcell));
            cells.add(8, Integer.toString(subcell + 10));
        }
        // middle cells
        else {
            cells.add(0, Integer.toString(subcell - 9));
            cells.add(1, Integer.toString(subcell + 1));
            cells.add(2, Integer.toString(subcell + 11));
            cells.add(3, Integer.toString(subcell - 10));
            cells.add(4, Integer.toString(subcell));
            cells.add(5, Integer.toString(subcell + 10));
            cells.add(6, Integer.toString(subcell - 11));
            cells.add(7, Integer.toString(subcell - 1));
            cells.add(8, Integer.toString(subcell + 9));
        }

        return cells;
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

